{% include "./external-links.md" %}
# OML Glossary Summary

The vocabulary of the Ontological Modeling Language, OML, consists of 91 definitions
(36 abstract and 55 concrete).
This OML vocabulary is the basis of the Ontological Modeling Framework (OMF), which is 
a collection of multiple technology-based Application Programming Interfaces (APIs) & libraries.

- **EMF/CDO** OMF APIs and libraries based on the [Eclipse Modeling Framework] and [Connected Data Objects]

  All 91 definitions induce corresponding EMF-based APIs and libraries.
  For the 55 concrete definitions, the *EMF/CDO* APIs
  include all the 50 *Normalized* APIs, all the 2 *Functional* APIs,
  and 3 definitions uniquely intended for *EMF/CDO*.
  
- **Normalized** OMF APIs and libraries based on polyglot functional programming in Java, JavaScript and Scala

  A subset of 50 definitions from the 55 concrete definitions
  constitute the set of normalized relational database schema tables for the technology-agnostic OML tabular interchange representation.
  These definitions generate language-friendly functional programming APIs for Java, JavaScript and Scala.
  Note that Scala is the only language that can provide strong compile-time guarantees of the referential transparency of the OML functional APIs.
  For Java and JavaScript, the OML functional APIs are intended to be referentially transparent; 
  however, these languages do not provide any guarantees at compile time or runtime for preserving these properties.
  
- **Functional** OMF APIs and libraries in Scala for in-memory processing of OML tabular interchange representations

  A subset of 2 definitions from the 55 concrete definitions
  augment the normalized OMF APIs for the in-memory processing of OMF information
  extracted from parsing the OML tabular interchange representation.


# 1 OML Common Glossary {#oml-common-glossary}
# 1.1 OML Common Glossary of 4 Abstract Definitions {#oml-common-abstract-glossary}
## OML Module

An OML Module maps to an [OWL2-DL Ontology];
it is a kind of OML Resource that is a logical container of OML ModuleElement(s)
and a non-logical container of OML Annotation(s).

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML Element
 - OML Resource


## OML ModuleEdge

An OML ModuleEdge is a binary, directed relationship from one OML Module to another

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Element


## OML ModuleElement

An OML ModuleElement is an OML Element defined in an OML Module

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Element


## OML Resource

An OML Resource is an abstraction for
everything in a vocabulary that is identifiable
locally by name within the vocabulary and
globally by an IRI across multiple vocabularies.
For a particular OML Resource, the constraints
between its name and its IRI depend on what kind of OML Resource it is.

{APIs: **Normalized**, **Functional**}

Abstract with 1 specialization:
 - OML Module

# 1.2 OML Common Glossary of 7 Concrete Definitions {#oml-common-concrete-glossary}

# 1.2.1 OML Common Glossary of 2 Schema Concrete Definitions {#oml-common-schema-concrete-glossary}

## OML AnnotationEntry

For the OML tabular interchange representation,
an OML AnnotationEntry (for a given OML AnnotationProperty) is a triple:
- an OML Module in which the OML AnnotationEntry appears
- an annotated OML TerminologyThing subject
- a String value as the representation of some information
  about the subject in that module.

{APIs: **Normalized**, **Functional**}

Normalized Relational Schema Table:
- moduleUUID: UUID (Foreign Key for: OML Module)
- subjectUUID: UUID (Foreign Key for: OML Element)
- value: String

## OML AnnotationProperty

An OML AnnotationProperty maps to an [OWL2 AnnotationProperty]
and is similarly a non-logical property for associating some information
to any OML TerminologyThing in an OML Module.

{APIs: **Normalized**, **Functional**}

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- iri: IRI
- abbrevIRI: AbbrevIRI

# 1.2.2 OML Common Glossary of 2 Functional API Concrete Definitions {#oml-common-functional-concrete-glossary}

## OML Annotation

An OML Annotation maps to an [OWL2 Annotation] and is similarly
a non-logical statement in an OML Module
associating some information as the value of an
OML AnnotationProperty for describing a subject (an OML TerminologyThing).

{APIs: **Functional**}

## OML Extent

An OML Extent is an in-memory store of all OML Element(s)
loaded from external OML documents.

{APIs: **Functional**}

# 1.2.3 OML Common Glossary of 3 EMF/CDO API Concrete Definitions {#oml-common-emf-cdo-concrete-glossary}

## OML AnnotationPropertyTable

For the OML tabular interchange representation,
an OML AnnotationPropertyTable pairs an OML AnnotationProperty key
with a set of OML AnnotationEntry values.

{APIs: **EMF/CDO**}

## OML AnnotationSubjectPropertyValue

An OML AnnotationSubjectPropertyValue is an in-memory construct
pairing an OML AnnotationProperty with a String value.

{APIs: **EMF/CDO**}

## OML AnnotationSubjectTable

An OML AnnotationSubjectTable is an in-memory construct
pairing an OML TerminologyThing subject key with a set
of OML AnnotationSubjectPropertyValue tuples.

{APIs: **EMF/CDO**}

# 2 OML Terminologies Glossary {#oml-terminologies-glossary}
# 2.1 OML Terminologies Glossary of 24 Abstract Definitions {#oml-terminologies-abstract-glossary}
## OML ConceptualEntity

An OML ConceptualEntity is an OML Entity that can be instantiated
as an OML ConceptualEntitySingletonInstance in any OML DescriptionBox.
It is partially instantiated if some essential OML EntityRelationship
or OML DataRelationshipFromEntity with `isIdentityCriteria=true` lacks
an OML TerminologyInstanceAssertion specifying its reference or value respectively.

{APIs: **Normalized**, **Functional**}

Abstract with 2 specializations:
 - OML Concept
 - OML ReifiedRelationship

## OML DataRange

An OWL DataRange corresponds to an [OWL2 DataRange] with arity=1.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Datatype

and with 2 specializations:
 - OML RestrictedDataRange
 - OML Scalar

## OML DataRelationship

An OML DataRelationship is an OML DirectedBinaryRelationshipKind
where the domain or the range is some kind of OML Datatype.

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML DirectedBinaryRelationshipKind
 - OML Term

and with 4 specializations:
 - OML EntityScalarDataProperty
 - OML EntityStructuredDataProperty
 - OML ScalarDataProperty
 - OML StructuredDataProperty

## OML DataRelationshipDomain

An OML DataRelationshipDomain is an abstraction
for the domain of an OML DataRelationship

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML Element
 - OML Resource

and with 2 specializations:
 - OML DataRelationshipFromEntity
 - OML DataRelationshipFromStructure

## OML DataRelationshipFromEntity

An OML DataRelationshipFromEntity is an OML DataRelationship
whose domain is an OML Entity.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML DataRelationshipDomain

and with 2 specializations:
 - OML EntityScalarDataProperty
 - OML EntityStructuredDataProperty

## OML DataRelationshipFromStructure

An OML DataRelationshipFromStructure is an OML DataRelationship
whose domain is an OML Structure.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML DataRelationshipDomain

and with 2 specializations:
 - OML ScalarDataProperty
 - OML StructuredDataProperty

## OML DataRelationshipRange

An OML DataRelationshipRange is an abstraction
for the range of an OML DataRelationship

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML Element
 - OML Resource

and with 2 specializations:
 - OML DataRelationshipToScalar
 - OML DataRelationshipToStructure

## OML DataRelationshipToScalar

An OML DataRelationshipToScalar is an OML DataRelationship
whose range is an OML DataRange.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML DataRelationshipRange

and with 2 specializations:
 - OML EntityScalarDataProperty
 - OML ScalarDataProperty

## OML DataRelationshipToStructure

An OML DataRelationshipToStructure is an OML DataRelationship
whose range is an OML Structure.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML DataRelationshipRange

and with 2 specializations:
 - OML EntityStructuredDataProperty
 - OML StructuredDataProperty

## OML Datatype

An OML Datatype is a common abstraction for what should be, in principle,
[OWL2 DataRange] of arity 1 (atomic) or > 1 (tuple structure).
In practice, an OML DataType maps to the [OWL2-DL] subset; which means
that an atomic OML Datatype maps to an [OWL2 Datatype] whereas
a structured OML Datatype maps to an [OWL2 Class].

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Term

and with 2 specializations:
 - OML DataRange
 - OML Structure

## OML DirectedBinaryRelationshipKind

An OML DirectedBinaryRelationshipKind is an abstraction
for the category of OML Term(s)
that are relationships with arity 2

{APIs: **Normalized**, **Functional**}

Abstract with 2 specializations:
 - OML DataRelationship
 - OML EntityRelationship

## OML Entity

An OML Entity is an abstraction for an OML Term
that is either an OML UnaryTermKind or
an OML DirectedBinaryRelationshipKind whose subject
and range are both a kind of OML Entity.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Term

and with 3 specializations:
 - OML Aspect
 - OML Concept
 - OML ReifiedRelationship

## OML EntityRelationship

An OML EntityRelationship is a kind of OML Term that
is an OML DirectedBinaryRelationshipKind between a
domain OML Entity and a range OML Entity.

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML DirectedBinaryRelationshipKind
 - OML Term

and with 2 specializations:
 - OML ReifiedRelationship
 - OML UnreifiedRelationship

## OML EntityRestrictionAxiom

An OML EntityRestrictionAxiom maps to an [OWL2 Object Property Restiction]
for an OML EntityRelationship.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML TermAxiom

and with 2 specializations:
 - OML EntityExistentialRestrictionAxiom
 - OML EntityUniversalRestrictionAxiom

## OML EntityScalarDataPropertyRestrictionAxiom

An OML EntityScalarDataPropertyRestrictionAxiom maps to
some kind of OWL2 Data Property Restriction.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML TermAxiom

and with 3 specializations:
 - OML EntityScalarDataPropertyExistentialRestrictionAxiom
 - OML EntityScalarDataPropertyParticularRestrictionAxiom
 - OML EntityScalarDataPropertyUniversalRestrictionAxiom

## OML RestrictedDataRange

An OML RestrictedDataRange corresponds to an [OWL2 DataRange] defined
in terms of some kind of restriction of some other OML DataRange.
The specializations of OML RestrictedDataRange correspond to
the allowed restrictions in the [OWL2 Datatype Maps].
Node that the vocabulary of XSD fundamental facets is not included in OWL2-DL, consequently,
there is no support in OML for specifying datatype restrictions involving XSD fundamental facets as well.
Each specialization maps to an OWL2 Declaration of an [OWL2 Datatype] whose
[OWL2 DataRange] corresponds to the OWL2 mapping of that specialized restriction.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML DataRange

and with 8 specializations:
 - OML BinaryScalarRestriction
 - OML IRIScalarRestriction
 - OML NumericScalarRestriction
 - OML PlainLiteralScalarRestriction
 - OML ScalarOneOfRestriction
 - OML StringScalarRestriction
 - OML SynonymScalarRestriction
 - OML TimeScalarRestriction

## OML SpecializationAxiom

An OML SpecializationAxiom is a logical axiom
about a taxonomic relationship between a specific and a general OML Entity.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML TermAxiom

and with 3 specializations:
 - OML AspectSpecializationAxiom
 - OML ConceptSpecializationAxiom
 - OML ReifiedRelationshipSpecializationAxiom

## OML Term

An OML Term map to the declaration of an [OWL2-DL Entity] of some kind.

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML Resource
 - OML TerminologyBoxStatement

and with 4 specializations:
 - OML DataRelationship
 - OML Datatype
 - OML Entity
 - OML EntityRelationship

## OML TermAxiom

An OML TermAxiom is a logical axiom about an OML Term.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML TerminologyBoxStatement

and with 4 specializations:
 - OML EntityRestrictionAxiom
 - OML EntityScalarDataPropertyRestrictionAxiom
 - OML ScalarOneOfLiteralAxiom
 - OML SpecializationAxiom

## OML TerminologyAxiom

An OML TerminologyAxiom is asserted in an OML TerminologyBox of some kind.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML ModuleEdge

and with 1 specialization:
 - OML TerminologyBoxAxiom

## OML TerminologyBox

An OML TerminologyBox is an OML Module for defining a domain-specific vocabulary
as a logical set of OML TerminologyBoxStatement(s), possibly by reuse of other
vocabularies via OML TerminologyBoxAxiom(s).
The semantics of an OML TerminologyBox domain-specific vocabulary is defined
by the mapping to [OWL2-DL] of the other vocabularies it reuses, if any, and
that of its OML TerminologyBoxAxiom(s) and OML TerminologyBoxStatement(s)
according to its OML TerminologyKind.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Module


## OML TerminologyBoxAxiom

An OML TerminologyBoxAxiom is an OML TerminologyAxiom that
asserts a logical statement about an OML TerminologyBox.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML TerminologyAxiom

and with 1 specialization:
 - OML TerminologyExtensionAxiom

## OML TerminologyBoxStatement

An OML TerminologyBoxStatement is a logical axiom about an OML TerminologyThing
in an OML TerminologyBox.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML ModuleElement

and with 2 specializations:
 - OML Term
 - OML TermAxiom

## OML UnaryTermKind

An OML UnaryTermKind is an abstraction for the category of OML Term(s)
that are relationships with arity 1

{APIs: **Normalized**, **Functional**}

Abstract with 4 specializations:
 - OML Aspect
 - OML Concept
 - OML Scalar
 - OML Structure

# 2.2 OML Terminologies Glossary of 28 Concrete Definitions {#oml-terminologies-concrete-glossary}

# 2.2.1 OML Terminologies Glossary of 28 Schema Concrete Definitions {#oml-terminologies-schema-concrete-glossary}

## OML TerminologyExtensionAxiom

An OML TerminologyExtensionAxiom allows an extendingTerminology to
make references (via OML TerminologyBoxStatement(s)) to OML TerminologyThing(s)
declared within the transitive closure of the extendedTerminlogy.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyBoxAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- extendedTerminologyUUID: UUID (Foreign Key for: OML TerminologyBox)

## OML Aspect

An OML Aspect is a kind of OML Entity of OML UnaryTermKind.
It is intended to be used as a specialization parent
for one or more OML ConceptualEntity.

{APIs: **Normalized**, **Functional**}

Concrete definition with 2 generalizations:
 - OML Entity
 - OML UnaryTermKind

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- name: LocalName

## OML Concept

An OML Concept is an OML ConceptualEntity of OML UnaryTermKind.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML ConceptualEntity
 - OML Entity
 - OML UnaryTermKind

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- name: LocalName

## OML ReifiedRelationship

An OML ReifiedRelationship is an OML ConceptualEntity and a kind of OML EntityRelationship
where an instance has an intrinsic identity. This means that
an OML ReifiedRelationship can be involved as the domain or the
range of another OML EntityRelationship as well as the
domain of an OML DataRelationshipFromEntity.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML ConceptualEntity
 - OML Entity
 - OML EntityRelationship

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- sourceUUID: UUID (Foreign Key for: OML Entity)
- targetUUID: UUID (Foreign Key for: OML Entity)
- isAsymmetric: Boolean
- isEssential: Boolean
- isFunctional: Boolean
- isInverseEssential: Boolean
- isInverseFunctional: Boolean
- isIrreflexive: Boolean
- isReflexive: Boolean
- isSymmetric: Boolean
- isTransitive: Boolean
- name: LocalName
- unreifiedPropertyName: LocalName
- unreifiedInversePropertyName: Option[LocalName]

## OML UnreifiedRelationship

An OML UnreifiedRelationship is a kind of OML EntityRelationship
where an instance has no intrinsic identity but rather structural
equivalence semantics as a tuple of references. This means
that an OML UnreifiedRelationship cannot be involved as the domain
or range of any kind of OML DirectedBinaryRelationshipKind.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML EntityRelationship

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- sourceUUID: UUID (Foreign Key for: OML Entity)
- targetUUID: UUID (Foreign Key for: OML Entity)
- isAsymmetric: Boolean
- isEssential: Boolean
- isFunctional: Boolean
- isInverseEssential: Boolean
- isInverseFunctional: Boolean
- isIrreflexive: Boolean
- isReflexive: Boolean
- isSymmetric: Boolean
- isTransitive: Boolean
- name: LocalName

## OML Scalar

An OML Scalar corresponds to an OWL2 Declaration of an [OWL2 DataRange] with arity=1.

{APIs: **Normalized**, **Functional**}

Concrete definition with 2 generalizations:
 - OML DataRange
 - OML UnaryTermKind

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- name: LocalName

## OML Structure

An OML Structure would correspond to an OWL2 Declaration of an [OWL2 DataRange] with arity > 1.
However, since OWL2 Datatypes in the [OWL2-DL] are restricted to have arity=1,
the OML mapping to [OWL2-DL] involves a pattern-based usage of an [OWL2 Class] to represent an OML Structure.
The arity corresponds to the cardinality of the set of ScalarDataProperty & StructuredDataProperty
relationships whose domain is this structure.

{APIs: **Normalized**, **Functional**}

Concrete definition with 2 generalizations:
 - OML Datatype
 - OML UnaryTermKind

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- name: LocalName

## OML BinaryScalarRestriction

An OML BinaryScalarRestriction is a data range that specifies how one binary scalar adds facet restrictions to another.
Applicable when the restricted scalar represents [OWL2 Binary Data].

The restricted scalar must be directly or indirectly a restriction of:
- [xsd:base64Binary]
- [xsd:hexBinary]

Facets:
- [xsd:length]
- [xsd:minLength]
- [xsd:maxLength]

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- length: Option[Int]
- minLength: Option[Int]
- maxLength: Option[Int]
- name: LocalName

## OML IRIScalarRestriction

An OML IRIScalarRestriction is a data range that specifies how one IRI scalar adds facet restrictions to another.
Applicable when the restricted scalar represents an [OWL2 IRI].

The restricted scalar must be directly or indirectly a restriction of:
- [xsd:anyURI]

Facets:
- [xsd:length]
- [xsd:minLength]
- [xsd:maxLength]
- [xsd:pattern]

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- length: Option[Int]
- minLength: Option[Int]
- maxLength: Option[Int]
- name: LocalName
- pattern: Option[Pattern]

## OML NumericScalarRestriction

An OML NumericScalarRestriction is a data range that specifies how one numeric scalar range adds facet restrictions to another.
Applies when the restricted scalar represents [OWL2 Real Numbers, Decimal Numbers and Integers] or [OWL2 Floating-Point Numbers].

The restricted scalar must be directly or indirectly a restriction of:
- [owl:real]
- [owl:rational]
- [xsd:decimal]
- [xsd:integer]
- [xsd:nonNegativeInteger]
- [xsd:nonPositiveInteger]
- [xsd:positiveInteger]
- [xsd:negativeInteger]
- [xsd:long]
- [xsd:int]
- [xsd:short]
- [xsd:byte]
- [xsd:unsignedLong]
- [xsd:unsignedInt]
- [xsd:unsignedShort]
- [xsd:unsignedByte]
- [xsd:double]
- [xsd:float]

Facets:
- [xsd:minInclusive]
- [xsd:maxInclusive]
- [xsd:minExclusive]
- [xsd:maxExclusive]

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- minExclusive: Option[LexicalNumber]
- minInclusive: Option[LexicalNumber]
- maxExclusive: Option[LexicalNumber]
- maxInclusive: Option[LexicalNumber]
- name: LocalName

## OML PlainLiteralScalarRestriction

An OML PlainLiteralScalarRestriction is a data range that specifies how one plain literal scalar adds facet restrictions to another.
Applicable when the restricted scalar represents [rdf:PlainLiteral].

The restricted scalar must be directly or indirectly a restriction of:
- [rdf:PlainLiteral]

Facets:
- [xsd:langRange]
- [xsd:length]
- [xsd:minLength]
- [xsd:maxLength]
- [xsd:pattern]

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- length: Option[Int]
- minLength: Option[Int]
- maxLength: Option[Int]
- name: LocalName
- langRange: Option[LangRange]
- pattern: Option[Pattern]

## OML ScalarOneOfRestriction

An OML ScalarOneOfRestriction is a data range that specifies how a scalar is a restricted set of literal values of another.
Semantics: OWL2 DataOneOf

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- name: LocalName

## OML StringScalarRestriction

An OML StringScalarRestriction is a data range that specifies how one string scalar adds facet restrictions to another.
Applicable when the restricted scalar represents [OWL2 Strings].

The restricted scalar must be directly or indirectly a restriction of:
- [xsd:string]
- [xsd:normalizedString]
- [xsd:token]
- [xsd:language]
- [xsd:Name]
- [xsd:NCName]
- [xsd:NMTOKEN]

Facets:
- [xsd:length]
- [xsd:minLength]
- [xsd:maxLength]
- [xsd:pattern]

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- length: Option[Int]
- minLength: Option[Int]
- maxLength: Option[Int]
- name: LocalName
- pattern: Option[Pattern]

## OML SynonymScalarRestriction

An OML SynonymScalarRestriction is a data range that is defined as a synonym for another (i.e. the restrictedRange).

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- name: LocalName

## OML TimeScalarRestriction

An OML TimeScalarRestriction is a data range that specifies how one time scalar adds facet restrictions to another.
Applicable when the restricted scalar represents [OWL2 Time Instants].

The restricted scalar must be directly or indirectly a restriction of:
- [xsd:dateTime]
- [xsd:dateTimeStamp]

Facets:
- [xsd:minInclusive]
- [xsd:maxInclusive]
- [xsd:minExclusive]
- [xsd:maxExclusive]

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML RestrictedDataRange

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRangeUUID: UUID (Foreign Key for: OML DataRange)
- minExclusive: Option[LexicalTime]
- minInclusive: Option[LexicalTime]
- maxExclusive: Option[LexicalTime]
- maxInclusive: Option[LexicalTime]
- name: LocalName

## OML EntityScalarDataProperty

An OML EntityScalarDataProperty is an OML DataRelationship
whose domain is an OML Entity and range is an OML DataRange.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML DataRelationship
 - OML DataRelationshipFromEntity
 - OML DataRelationshipToScalar

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- domainUUID: UUID (Foreign Key for: OML Entity)
- rangeUUID: UUID (Foreign Key for: OML DataRange)
- isIdentityCriteria: Boolean
- name: LocalName

## OML EntityStructuredDataProperty

An OML EntityStructuredDataProperty is an OML DataRelationship
whose domain is an OML Entity and range is an OML Structure.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML DataRelationship
 - OML DataRelationshipFromEntity
 - OML DataRelationshipToStructure

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- domainUUID: UUID (Foreign Key for: OML Entity)
- rangeUUID: UUID (Foreign Key for: OML Structure)
- isIdentityCriteria: Boolean
- name: LocalName

## OML ScalarDataProperty

An OML ScalarDataProperty is an OML DataRelationship
whose domain is an OML Structure and range is an OML DataRange.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML DataRelationship
 - OML DataRelationshipFromStructure
 - OML DataRelationshipToScalar

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- domainUUID: UUID (Foreign Key for: OML Structure)
- rangeUUID: UUID (Foreign Key for: OML DataRange)
- name: LocalName

## OML StructuredDataProperty

An OML StructuredDataProperty is an OML DataRelationship
whose domain is an OML Structure and range is an OML Structure.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML DataRelationship
 - OML DataRelationshipFromStructure
 - OML DataRelationshipToStructure

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- domainUUID: UUID (Foreign Key for: OML Structure)
- rangeUUID: UUID (Foreign Key for: OML Structure)
- name: LocalName

## OML AspectSpecializationAxiom

An OML AspectSpecializationAxiom is a logical axiom
about a taxonomic relationship between a specific OML Aspect
and a general OML Entity.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML SpecializationAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- superAspectUUID: UUID (Foreign Key for: OML Aspect)
- subEntityUUID: UUID (Foreign Key for: OML Entity)

## OML ConceptSpecializationAxiom

An OML ConceptSpecializationAxiom is a logical axiom
about a taxonomic relationship between a specific and general
OML Concept.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML SpecializationAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- superConceptUUID: UUID (Foreign Key for: OML Concept)
- subConceptUUID: UUID (Foreign Key for: OML Concept)

## OML ReifiedRelationshipSpecializationAxiom

An OML ReifiedRelationshipSpecializationAxiom is a logical axiom
about a taxonomic relationship between a specific and general
OML ReifiedRelationship.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML SpecializationAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- superRelationshipUUID: UUID (Foreign Key for: OML ReifiedRelationship)
- subRelationshipUUID: UUID (Foreign Key for: OML ReifiedRelationship)

## OML EntityExistentialRestrictionAxiom

An OML EntityExistentialRestrictionAxiom maps
to an [OWL2 Object Property Existential Restriction].

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML EntityRestrictionAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRelationUUID: UUID (Foreign Key for: OML EntityRelationship)
- restrictedDomainUUID: UUID (Foreign Key for: OML Entity)
- restrictedRangeUUID: UUID (Foreign Key for: OML Entity)

## OML EntityUniversalRestrictionAxiom

An OML EntityUniversalRestrictionAxiom maps
to an [OWL2 Object Property Universal Restriction].

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML EntityRestrictionAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedRelationUUID: UUID (Foreign Key for: OML EntityRelationship)
- restrictedDomainUUID: UUID (Foreign Key for: OML Entity)
- restrictedRangeUUID: UUID (Foreign Key for: OML Entity)

## OML EntityScalarDataPropertyExistentialRestrictionAxiom

An OML EntityScalarDataPropertyExistentialRestrictionAxiom maps to an
OWL2 DataSomeValuesFrom restriction (the range must be explicitly defined as a Scalar datatype)

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML EntityScalarDataPropertyRestrictionAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedEntityUUID: UUID (Foreign Key for: OML Entity)
- scalarPropertyUUID: UUID (Foreign Key for: OML EntityScalarDataProperty)
- scalarRestrictionUUID: UUID (Foreign Key for: OML DataRange)

## OML EntityScalarDataPropertyParticularRestrictionAxiom

*
An OML EntityScalarDataPropertyParticularRestrictionAxiom maps to an OWL2 DataHasValue restriction.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML EntityScalarDataPropertyRestrictionAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedEntityUUID: UUID (Foreign Key for: OML Entity)
- scalarPropertyUUID: UUID (Foreign Key for: OML EntityScalarDataProperty)
- literalValue: LexicalValue

## OML EntityScalarDataPropertyUniversalRestrictionAxiom

An OML EntityScalarDataPropertyUniversalRestrictionAxiom maps to an
OWL2 DataAllValuesFrom (the range must be explicitly defined as a Scalar datatype)

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML EntityScalarDataPropertyRestrictionAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- restrictedEntityUUID: UUID (Foreign Key for: OML Entity)
- scalarPropertyUUID: UUID (Foreign Key for: OML EntityScalarDataProperty)
- scalarRestrictionUUID: UUID (Foreign Key for: OML DataRange)

## OML ScalarOneOfLiteralAxiom

An OML ScalarOneOfLiteralAxiom specifies a literal in a ScalarOneOfRestriction data range.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TermAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- axiomUUID: UUID (Foreign Key for: OML ScalarOneOfRestriction)
- value: LexicalValue

# 3 OML Graphs Glossary {#oml-graphs-glossary}
# 3.1 OML Graphs Glossary of 3 Concrete Definitions {#oml-graphs-concrete-glossary}

# 3.1.1 OML Graphs Glossary of 3 Schema Concrete Definitions {#oml-graphs-schema-concrete-glossary}

## OML TerminologyGraph

An OML TerminologyGraph is an OML TerminologyBox with no statements our axioms involving OML Bundle(s).

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyBox

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- kind: TerminologyKind
- iri: IRI

## OML ConceptDesignationTerminologyAxiom

An OML ConceptDesignationTerminologyAxiom establishes
a relationship from a source OML TerminologyBox
where a designated OML Concept is defined to
a target OML TerminologyGraph in which the internal
structure of the designated OML Concept can be defined.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyBoxAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- designatedConceptUUID: UUID (Foreign Key for: OML Concept)
- designatedTerminologyUUID: UUID (Foreign Key for: OML TerminologyBox)

## OML TerminologyNestingAxiom

An OML TerminologyNestingAxiom provides support for relating
a white-box nested OML TerminologyGraph used for describing internal
details about a nesting OML Concept defined in a nesting OML TerminologyBox.
This nesting OML Concept specifies the context for the internal details
defined in the nested OML TerminologyGraph.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyBoxAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- nestingTerminologyUUID: UUID (Foreign Key for: OML TerminologyBox)
- nestingContextUUID: UUID (Foreign Key for: OML Concept)

# 4 OML Bundles Glossary {#oml-bundles-glossary}
# 4.1 OML Bundles Glossary of 4 Abstract Definitions {#oml-bundles-abstract-glossary}
## OML ConceptTreeDisjunction

An OML ConceptTreeDisjunction represents the root & non-leaf nodes of a concept taxonomy:
- Root node is a RootConceptTaxonomyAxiom.
- Non-leaf nodes are AnonymousConceptTaxonomyAxioms.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Element

and with 2 specializations:
 - OML AnonymousConceptUnionAxiom
 - OML RootConceptTaxonomyAxiom

## OML DisjointUnionOfConceptsAxiom

An OML DisjointUnionOfConceptsAxiom(s) specifies the logical union of non-leaf & leaf nodes in a concept taxonomy tree:
- Each non-leaf node is an OML AnonymousConceptUnionAxiom.
- Each leaf nodes is an OML SpecificDisjointConceptAxiom.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Element

and with 2 specializations:
 - OML AnonymousConceptUnionAxiom
 - OML SpecificDisjointConceptAxiom

## OML TerminologyBundleAxiom

An OML TerminologyBundleAxiom is a TerminologyAxiom that asserts a logical statement in an OML Bundle.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML TerminologyAxiom

and with 1 specialization:
 - OML BundledTerminologyAxiom

## OML TerminologyBundleStatement

An OML TerminologyBundleStatement is a logical axiom about an OML TerminologyThing
in an OML Bundle.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML ModuleElement

and with 1 specialization:
 - OML RootConceptTaxonomyAxiom

# 4.2 OML Bundles Glossary of 5 Concrete Definitions {#oml-bundles-concrete-glossary}

# 4.2.1 OML Bundles Glossary of 5 Schema Concrete Definitions {#oml-bundles-schema-concrete-glossary}

## OML Bundle

An OML Bundle is an OML TerminologyBox that is
an acyclic logical aggregate of other OML TerminologyBox(es)
and that logically assert OML TerminologyBundleStatement(s).

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyBox

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- kind: TerminologyKind
- iri: IRI

## OML BundledTerminologyAxiom

An OML BundledTerminologyAxiom identifies an OML TerminologyBox that an OML Bundle aggregates.
An OML BundledTerminologyAxiom allows an OML Bundle to
make references (via OML TerminologyStatement(s)) to OML TerminologyThing(s)
within the transitive closure of a bundledTerminology.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyBundleAxiom

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- bundledTerminologyUUID: UUID (Foreign Key for: OML TerminologyBox)

## OML RootConceptTaxonomyAxiom

An OML RootConceptTaxonomyAxiom asserts that, in the scope of a Bundle, a particular OML Concept
is the root of a taxonomy of concept specializations.

{APIs: **Normalized**, **Functional**}

Concrete definition with 2 generalizations:
 - OML ConceptTreeDisjunction
 - OML TerminologyBundleStatement

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- rootUUID: UUID (Foreign Key for: OML Concept)

## OML SpecificDisjointConceptAxiom

An OML SpecificDisjointConceptAxiom specifies a leaf in a concept taxonomy tree.

{APIs: **Normalized**, **Functional**}

Concrete definition with 2 generalizations:
 - OML DisjointUnionOfConceptsAxiom
 - OML Element

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- disjointLeafUUID: UUID (Foreign Key for: OML Concept)

## OML AnonymousConceptUnionAxiom

An OML AnonymousConceptUnionAxiom specifies an anonymous logical union of DisjointUnionOfEntityAxioms in a concept taxonomy tree.
Although it is semantically anonymous, it is syntactically identified with name whose
only purpose is for generating the axiom's namespace UUID.

{APIs: **Normalized**, **Functional**}

Concrete definition with 3 generalizations:
 - OML ConceptTreeDisjunction
 - OML DisjointUnionOfConceptsAxiom
 - OML Element

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- name: LocalName

# 5 OML Descriptions Glossary {#oml-descriptions-glossary}
# 5.1 OML Descriptions Glossary of 4 Abstract Definitions {#oml-descriptions-abstract-glossary}
## OML ConceptualEntitySingletonInstance

An OML ConceptualEntitySingletonInstance defines an instance of an OML ConceptualEntity.
An OML ConceptualEntitySingletonInstance maps to an [OWL2 NamedIndividual].
The semantics depends on the kind of OML ConceptualEntity classifier:
- If the OML ConceptualEntity is the domain for at least at least
  one OML DataRelationshipFromEntity with `isIdentityCriteria=true`,
  then the [OWL2-DL] mapping includes an [OWL2 Key Axiom]
  forcing that all distinctly named OML ConceptualEntitySingletonInstance
  must have different values for each OML DataRelationshipFromEntity with `isIdentityCriteria=true`.
- otherwise, distinctly named OML ConceptualEntitySingletonInstance
  represent semantically different instances.

{APIs: **Normalized**, **Functional**}

Abstract definition with 2 generalizations:
 - OML Resource
 - OML TerminologyInstanceAssertion

and with 2 specializations:
 - OML ConceptInstance
 - OML ReifiedRelationshipInstance

## OML DescriptionBoxRelationship

An OML DescriptionBoxRelationship is a directed binary relationship
from an OML DescriptionBox source to an OML Module target.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML ModuleEdge

and with 2 specializations:
 - OML DescriptionBoxExtendsClosedWorldDefinitions
 - OML DescriptionBoxRefinement

## OML SingletonInstanceStructuredDataPropertyContext

An OML SingletonInstanceStructuredDataPropertyContext defines the context of
an OML DataRelationshipToStructure for an insance of either an OML Concept or OML Structure
for specifying values of its data properties
via nested OML StructuredDataPropertyTuple(s) and OML ScalarDataPropertyValue(s).

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML Element

and with 2 specializations:
 - OML SingletonInstanceStructuredDataPropertyValue
 - OML StructuredDataPropertyTuple

## OML TerminologyInstanceAssertion

An OML TerminologyInstanceAssertion is a logical OML ModuleElement defined in an OML TerminologyDescription.

{APIs: **Normalized**, **Functional**}

Abstract definition with 1 generalization:
 - OML ModuleElement

and with 4 specializations:
 - OML ConceptualEntitySingletonInstance
 - OML ReifiedRelationshipInstanceDomain
 - OML ReifiedRelationshipInstanceRange
 - OML UnreifiedRelationshipInstanceTuple

# 5.2 OML Descriptions Glossary of 12 Concrete Definitions {#oml-descriptions-concrete-glossary}

# 5.2.1 OML Descriptions Glossary of 12 Schema Concrete Definitions {#oml-descriptions-schema-concrete-glossary}

## OML ConceptInstance

An OML ConceptInstance is an OML ConceptualEntitySingletonInstance classified by an OML Concept.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML ConceptualEntitySingletonInstance

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- singletonConceptClassifierUUID: UUID (Foreign Key for: OML Concept)
- name: LocalName

## OML DescriptionBox

An OML DescriptionBox maps to an [OWL2-DL Ontology]
about [OWL2-DL NamedIndividuals] mapped from OML TerminologyInstanceAssertion(s).

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML Module

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- kind: DescriptionKind
- iri: IRI

## OML DescriptionBoxExtendsClosedWorldDefinitions

An OML DescriptionBoxExtendsClosedWorldDefinition specifies
an OML DescriptionBoxRelationship from an OML DescriptionBox
to an OML TerminologyBox such that an OML SingletonInstance defined in the
former can be an instance of an OML Term defined in the latter.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML DescriptionBoxRelationship

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- closedWorldDefinitionsUUID: UUID (Foreign Key for: OML TerminologyBox)

## OML DescriptionBoxRefinement

An OML DescriptionBoxRefinement is an OML DescriptionBoxRelationship
from an OML DescriptionBox to another OML DescriptionBox where
the former refines the descriptions of the latter.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML DescriptionBoxRelationship

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- refinedDescriptionBoxUUID: UUID (Foreign Key for: OML DescriptionBox)

## OML ReifiedRelationshipInstance

An OML ReifiedRelationshipInstance is an OML ConceptualEntitySingletonInstance  classified by an OML ReifiedRelationship.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML ConceptualEntitySingletonInstance

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- singletonReifiedRelationshipClassifierUUID: UUID (Foreign Key for: OML ReifiedRelationship)
- name: LocalName

## OML ReifiedRelationshipInstanceDomain

An OML ReifiedRelationshipInstanceDomain specifies which OML ConceptualEntitySingletonInstance
plays the role of the domain for an OML ReifiedRelationshipInstance.
An OML ReifiedRelationshipInstanceDomain has no intrinsic identity; instead,
an OML ReifiedRelationshipInstanceDomain is semantically equivalent
to another OML ReifiedRelationshipInstanceDomain referencing the same property and domain.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyInstanceAssertion

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- reifiedRelationshipInstanceUUID: UUID (Foreign Key for: OML ReifiedRelationshipInstance)
- domainUUID: UUID (Foreign Key for: OML ConceptualEntitySingletonInstance)

## OML ReifiedRelationshipInstanceRange

An OML ReifiedRelationshipInstanceRange specifies which OML ConceptualEntitySingletonInstance
plays the role of the range for an OML ReifiedRelationshipInstance.
An OML ReifiedRelationshipInstanceRange has no intrinsic identity; instead,
an OML ReifiedRelationshipInstanceRange is semantically equivalent
to another OML ReifiedRelationshipInstanceRange referencing the same property and range.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyInstanceAssertion

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- reifiedRelationshipInstanceUUID: UUID (Foreign Key for: OML ReifiedRelationshipInstance)
- rangeUUID: UUID (Foreign Key for: OML ConceptualEntitySingletonInstance)

## OML ScalarDataPropertyValue

An OML ScalarDataPropertyValue specifies a literal string as the value of
an OML DataRelationshipToScalar in the scope of an OML SingletonInstanceStructuredDataPropertyContext.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML Element

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- scalarDataPropertyUUID: UUID (Foreign Key for: OML DataRelationshipToScalar)
- scalarPropertyValue: String

## OML SingletonInstanceScalarDataPropertyValue

An OML SingletonInstanceScalarDataPropertyValue defines a tuple for representing the atomic String value
of an OML EntityScalarDataProperty for a particular OML ConceptualEntitySingletonInstance.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML ModuleElement

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- singletonInstanceUUID: UUID (Foreign Key for: OML ConceptualEntitySingletonInstance)
- scalarDataPropertyUUID: UUID (Foreign Key for: OML EntityScalarDataProperty)
- scalarPropertyValue: String

## OML SingletonInstanceStructuredDataPropertyValue

An OML SingletonInstanceStructuredDataPropertyValue is an SingletonInstanceStructuredDataPropertyContext
for an OML ConceptualEntitySingletonInstance.

{APIs: **Normalized**, **Functional**}

Concrete definition with 2 generalizations:
 - OML ModuleElement
 - OML SingletonInstanceStructuredDataPropertyContext

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- singletonInstanceUUID: UUID (Foreign Key for: OML ConceptualEntitySingletonInstance)
- structuredDataPropertyUUID: UUID (Foreign Key for: OML DataRelationshipToStructure)

## OML StructuredDataPropertyTuple

An OML StructuredDataPropertyTuple is an SingletonInstanceStructuredDataPropertyContext
nested in a container OML SingletonInstanceStructuredDataPropertyContext.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML SingletonInstanceStructuredDataPropertyContext

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- structuredDataPropertyUUID: UUID (Foreign Key for: OML DataRelationshipToStructure)

## OML UnreifiedRelationshipInstanceTuple

An OML UnreifiedRelationshipInstanceTuple specifies a triple involving
a reference to an OML ConceptualEntitySingletonInstance playing the role of the domain
of an OML UnreifiedRelationship whose range is played by a reference to an OML ConceptualEntitySingletonInstance.
An OML UnreifiedRelationshipInstanceTuple has no intrinsic identity; instead,
an OML UnreifiedRelationshipInstanceTuple is semantically equivalent
to another OML UnreifiedRelationshipInstanceTuple referencing the same domain, property and range.

{APIs: **Normalized**, **Functional**}

Concrete definition with 1 generalization:
 - OML TerminologyInstanceAssertion

Normalized Relational Schema Table:
- uuid: UUID (Primary Key)
- unreifiedRelationshipUUID: UUID (Foreign Key for: OML UnreifiedRelationship)
- domainUUID: UUID (Foreign Key for: OML ConceptualEntitySingletonInstance)
- rangeUUID: UUID (Foreign Key for: OML ConceptualEntitySingletonInstance)
