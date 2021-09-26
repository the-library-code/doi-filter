[![DSpace Logo](https://the-library-code.de/dspace_logo.png)](http://www.dspace.org)

[![The Library Code GmbH](https://the-library-code.de/the_library_code_gmbh.png)](https://www.the-library-code.de)

# DOI Filter

## Introduction

This is an Open Source plugin for [DSpace](https://www.dspace.org) version 6.3. This feature allows selective filtering of items for DOI minting, as well as ways for administrators to override/skip those filters. It brings with it a complete generic logical filtering system for DSpace items (similar in concept to the XOAI XML filter definitions) that can be used by any other service or component of DSpace.

We are providing this as a plugin to DSpace 6.3 as DSpace 6.x releases no longer include new features, only bug fixes. This feature will be included in DSpace 7.1 out of the box. Please contact us if you need a version of this plugin for DSpace 5.

This plugin was developed by [The Library Code GmbH](https://www.the-library-code.de) with the support of [Technische Informationsbibliothek (TIB)](https://www.tib.eu), the [Technische Universität Hamburg TUHH](https://www.tuhh.de), and the [Hessische Bibliotheksinformationssystem hebis](https://www.hebis.de).

## General idea

Out of the box, DSpace mints DOIs automatically for all items or for none, depending on its configuration. While this suits many repositories, many other repositories would like to mint DOIs for certain items only. This plugin adds logical filters to select items based on tests and boolean logic. Before DSpace mints a new DOI for an item it uses a configured filter to test whether this item should be processed (true) or not (false). While this plugin uses the logical filters for DOI minting only, the filters can also be used by any other service or component of DSpace. The filters are defined in a spring configuration and were inspired by the XML filter definitions of XOAI.

## Installation

To install this plugin simply cherry-pick the following three commits from this git repository: [c8e5587](https://github.com/the-library-code/doi-filter/commit/c8e5587c100c33e5e8ad89c26ceb530243b99919), [006522b](https://github.com/the-library-code/doi-filter/commit/006522b0c6100fc8ea7677222ae7064ccd0e3342), and [6d0b11c](https://github.com/the-library-code/doi-filter/commit/6d0b11cf24862e00116605e0ac8adbb837b1df69). `c8e5587` contains the actual code changes. `006522b` will fail or create merge conflicts if you've activated minting of DOIs before. It just shows out how to reference the filter in `identifier-service.xml`. And `6d0b11c` contains changes to the English message catalog ony. Finally, configure the plugin as described in the next section and recompile and update your DSpace installation.

## Configuration
This plugins adds a new configuration file: `${dspace}/config/spring/api/item-filters.xml`. Within this file LogicalStatements, Filters, Operators, and Conditions can be defined. The file contains a ton of examples and comments. Once you have installed this plugin and defined a filter in `item-filters.xml`, you can reference the filter as part of your DOIConfiguration by adding the property `filterService` to the `DOIIdentifierProvider` or `VersionedDOIIdentifierProvider` in `${dspace}/config/spring/api/item-filters.xml`:

```xml
<bean id="org.dspace.identifier.DOIIdentifierProvider"
        class="org.dspace.identifier.DOIIdentifierProvider"
        scope="singleton">
    <property name="configurationService"
        ref="org.dspace.services.ConfigurationService" />
    <property name="DOIConnector"
        ref="org.dspace.identifier.doi.DOIConnector" />
    <property name="filterService" ref="simple-demo_filter" />
</bean>
```

Only items for which the filter returns `true` will get DOIs minted.

### More details about Conditions, Operators and Filters

Conditions are simple tests on an item. Custom conditions can be developed easily if you know Java (see below). This plugin come along with the following conditions on board:

* `BitstreamCountCondition` - Counts the number of bitstreams in a specified bundle, you can define a parameter `min`, `max`, and `bundle`.
* `InCollectionCondition` - Gets a list of handles of collections as parameter `collections` and return true if the item is in any of them.
* `InCommunityCondition` - Gets a list of handles of communities as parameter `communities`and return true if the item is in any of them.
* `IsWithdrawnCondition` - Checks if an item is withdrawn.
* `MetadataValueMatchCondition` - Needs a parameter `pattern` and a parameter `field` and checks if any value of the specified field matches the regular expression pattern. Note that DSpace uses Java and you have to follow Java's regular expression syntax.
* `MetadataValuesMatchCondition` - Same as the `MetadataValueMatchCondition` but for a list of patterns. If any pattern matches any value of the specified field, the condition returns `true`.
* `ReadableByGroupCondition` - You must specify the parameter `group` with a name of a group in DSpace and a parameter `action` (e.g. `READ`, `ADMIN`, ...). If the group is allowed to perform the action on the item, the condition returns true. This is often used for the Group `Anonymous` and the action `READ`to check for public access.
* `SimpleBooleanCondition` - Always returns `true` or `false` depending on the parameter `condition`.

You can then build Filters using these conditions and Operators. Operators are the operators of boolean logic: `And`, `Or`, and `Not`. We also implemented `Nand` and `Nor`. Use the operators to chain multiple conditions, building large filters. Every filter must have an  `id` that is used to reference it when applying the filter on services. There is only one filter implemented: `DefaultFilter`. Use spring configuration to create as many instances of the `DefaultFilter` as you need, specifying different filter conditions.

# Technical Details: Logical Item Filtering

If you want to use logical item filtering for other services or develop custom conditions, the following hopefuly helps you to better understand the concept and technical details of its implementation.

## DSpace Logical Item filtering (org.dspace.content.logic.*)
Inspired by the powerful conditional filters in XOAI, this component offers a simple but flexible way to write logical statements and tests, and use the results of those tests in other services or DSpace code.

### LogicalStatement
LogicalStatement is a simple interface ultimately implemented by all the other interfaces and classes described below. It just requires that a class implements a `Boolean getResult(context, item)` method.

### Filters
Filters are at the root of any test definition, and it is the filter ID that is used to load up the filter in spring configurations for other services, or with DSpace Service Manager.

A filter bean is defined with a single “statement” property - this could be an Operator, to begin a longer logical statement, or a Condition, to perform a simple check.

There is one simple implementation of Filter included - `DefaultFilter`. You probably won't need to implement your own Filter. But we need a class to represent filters in Java and to be able to configure them in Spring.

### Operators

Operators are the basic logical building blocks that implement operations like AND, OR, NOT, NAND and NOR. An Operator can contain any number of other Operators or Conditions.

So statements like this can be created: `(x AND (y OR z) AND a AND (b OR NOT(d))`

### Conditions
Conditions are where the actual DSpace item evaluation code is written. A condition accepts a `Map<String, Object>` map of parameters. Conditions don’t contain any other LogicalStatement classes – the are at the bottom of the chain.

A condition could be something like MetadataValueMatchCondition, where a regex pattern and field name are passed as parameters, then tested against actual item metadata. If the regex matches, the boolean result is true.

Typically, commonly used Conditions will be defined as beans elsewhere in the spring config and then referenced inside Filters and Operators to create more complex statements.

### Configuring Filters in Spring
Conditions, Operators and Filters are all defined in `${dspace}/config/spring/api/item-filters.xml`

### Running Tests on the Command Line
There is a launcher command that can arbitrarily run tests on an item or all items, eg.
`${dspace}/bin/dspace test-logic -f openaire_filter -i 123456789/100`. A simple true or false is printed for each item tested.

### Using Filters in other Spring Services
The Filter beans can be referenced (or defined) in other services, for instance, here is adding the bean we configured earlier, as a `filterService` to a new `FilteredDOIIdentifierProvider`:

```xml
<bean id="org.dspace.identifier.DOIIdentifierProvider"
      class="org.dspace.identifier.FilteredDOIIdentifierProvider"
      scope="singleton">
    <property name="configurationService"
              ref="org.dspace.services.ConfigurationService" />
    <property name="DOIConnector"
              ref="org.dspace.identifier.doi.DOIConnector" />
    <property name="filterService"
              ref="openaire_filter"/>
</bean>
```

In the provider, we just define the property with the other services and class variables:
`private Filter filterService;`

And make sure there is a setter for it:

```java
public void setFilterService(Filter filterService) { 
    this.filterService = filterService; 
}
```

Then you can actually run the tests with the service, like this:

```java
try {
    Boolean result = filterService.getResult(context, (Item) dso);
    // do something with result
} catch(LogicalStatementException e) {
    // ... handle exception ...
}
```

In the TestLogicRunner, you can see a way to get the filters by name using the DSpaceServiceManager as well.



# Extensive Configuration Example

Here’s a complete example of a filter definition that implements the same rules as the XOAI openAireFilter. As an exercise, some statements will be defined as beans externally, and some will be defined inline as part of the filter.

#### New Condition: `driver-document-type_condition`
This condition creates a new bean to test metadata values. In this case, we’re implementing “ends with” for a list of type patterns.

```xml
<!-- dc.type ends with any of the listed values, as per XOAI "driverDocumentTypeCondition" -->
    <bean id="driver-document-type_condition"
          class="org.dspace.content.logic.condition.MetadataValuesMatchCondition">
        <property name="parameters">
            <map>
                <entry key="field" value="dc.type" />
                <entry key="patterns">
                    <list>
                        <value>article$</value>
                        <value>bachelorThesis$</value>
                        <value>masterThesis$</value>
                        <value>doctoralThesis$</value>
                        <value>book$</value>
                        <value>bookPart$</value>
                        <value>review$</value>
                        <value>conferenceObject$</value>
                        <value>lecture$</value>
                        <value>workingPaper$</value>
                        <value>preprint$</value>
                        <value>report$</value>
                        <value>annotation$</value>
                        <value>contributionToPeriodical$</value>
                        <value>patent$</value>
                        <value>dataset$</value>
                        <value>other$</value>
                    </list>
                </entry>
            </map>
        </property>
    </bean>
```

#### New Condition: `item-is-public_condition`
This condition accepts group and action parameters, then inspects item policies for a match - if the supplied group can perform the action, the result is true.

```xml
<bean id="item-is-public_condition" class="org.dspace.content.logic.condition.ReadableByGroupCondition">
    <property name="parameters">
        <map>
            <entry key="group" value="Anonymous" />
            <entry key="action" value="READ" />
        </map>
    </property>
</bean>
```

#### New Filter: `openaire_filter`
Here is the full definition for the OpenAIRE filter:

* The first statement is an And Operator, with many sub-statements – four Conditions, and an Or statement.
  * The first two statements in this Operator are simple Conditions defined in-line, and just check for a non-empty value in a couple of metadata fields.
  * The third statement is a reference to the document type Condition we made earlier: `<ref bean="driver-document-type_condition" />`.
  * The fourth statement is another Operator, in this case an Or Operator with two Conditions:
     * the is-public Condition we defined earlier,
     * and an in-line definition of as “is-withdrawn” Condition).
  * The fifth statement is an in-line definition of a Condition that checks dc.relation metadata for a valid OpenAIRE identifier.

So the full logic implemented is: (has-title AND has-author AND has-driver-type AND (is-public OR is-withdrawn) AND has-valid-relation).

```xml
<!-- An example of an OpenAIRE compliance filter based on the same rules in xoai.xml
      some sub-statements are defined within this bean, and some are referenced from earlier definitions
-->
<bean id="openaire_filter" class="org.dspace.content.logic.DefaultFilter">
    <property name="statement">
        <bean class="org.dspace.content.logic.operator.And">
            <property name="statements">
                <list>
                    <!-- Has a non-empty title -->
                    <bean id="has-title_condition"
                          class="org.dspace.content.logic.condition.MetadataValueMatchCondition">
                        <property name="parameters">
                            <map>
                                <entry key="field" value="dc.title" />
                                <entry key="pattern" value=".*" />
                            </map>
                        </property>
                    </bean>
                    <!-- AND has a non-empty author -->
                    <bean id="has-author_condition"
                          class="org.dspace.content.logic.condition.MetadataValueMatchCondition">
                        <property name="parameters">
                            <map>
                                <entry key="field" value="dc.contributor.author" />
                                <entry key="pattern" value=".*" />
                            </map>
                        </property>
                    </bean>
                    <!-- AND has a valid DRIVER document type (defined earlier) -->
                    <ref bean="driver-document-type_condition" />
                    <!-- AND (the item is publicly accessible OR withdrawn) -->
                    <bean class="org.dspace.content.logic.operator.Or">
                        <property name="statements">
                            <list>
                                <!-- item is public, defined earlier -->
                                <ref bean="item-is-public_condition" />
                                <!-- OR item is withdrawn, for tombstoning -->
                                <bean class="org.dspace.content.logic.condition.IsWithdrawnCondition">
                                    <property name="parameters"><map></map></property>
                                </bean>
                            </list>
                        </property>
                    </bean>
                    <!-- AND the dc.relation is a valid OpenAIRE identifier
                          (starts with "info:eu-repo/grantAgreement/") -->
                    <bean id="has-openaire-relation_condition"
                          class="org.dspace.content.logic.condition.MetadataValueMatchCondition">
                        <property name="parameters">
                            <map>
                                <entry key="field" value="dc.relation" />
                                <entry key="pattern" value="^info:eu-repo/grantAgreement/" />
                            </map>
                        </property>
                    </bean>
                </list>
            </property>
        </bean>
    </property>
</bean>
```

