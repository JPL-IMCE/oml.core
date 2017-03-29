/*
 * Copyright 2016 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
 */

package gov.nasa.jpl.imce.oml.resolver

import java.util.UUID

import gov.nasa.jpl.imce.oml.{resolver, _}

import scalax.collection.immutable.Graph
import scala.collection.immutable.{Map, Seq, SortedSet, TreeSet}
import scala.collection.parallel.immutable.ParSeq
import scala.{Boolean, None, Ordering, Some, StringContext, Tuple2, Tuple3}
import scala.util.{Failure, Success, Try}
import scala.Predef.ArrowAssoc

case class OMLTablesResolver private[resolver]
(context: TerminologyContext,
 queue: tables.OMLSpecificationTables,
 factory: api.OMLResolvedFactory)

object OMLTablesResolver {

  def resolve
  (t: tables.OMLSpecificationTables,
   factory: api.OMLResolvedFactory,
   extentUUID: java.util.UUID)
  : Try[OMLTablesResolver]
  = for {
    init <- Try.apply(OMLTablesResolver(
      resolver.TerminologyContext(
        factory.createExtent(
          uuid = extentUUID,
          annotationProperties =
            t
              .annotationProperties
              .foldLeft[SortedSet[resolver.api.AnnotationProperty]](TreeSet.empty[resolver.api.AnnotationProperty]) {
              case (acc, ap) =>
                acc +
                  factory.createAnnotationProperty(ap.iri, ap.abbrevIRI)
            },
          modules = Map.empty[java.util.UUID, resolver.api.Module])),
      t,
      factory))
    // Terminologies
    step0a <- mapTerminologyGraphs(init)
    step0b<- mapBundles(step0a)
    // Atomic terms
    step1a <- mapAspects(step0b)
    step1b <- mapConcepts(step1a)
    step1c <- mapScalars(step1b)
    step1d <- mapStructures(step1c)
    // TerminologyBoxAxiom relationships
    step2a <- mapTerminologyExtends(step1d)
    step2b <- mapTerminologyNestings(step2a)
    step2c <- mapConceptDesignationTerminologyAxioms(step2b)
    // TerminologyBundleAxiom relationships
    step3a <- mapBundledTerminologyAxioms(step2c)
    // Relational terms
    step4a <- mapRestrictedDataRanges(step3a)
    step4b <- mapReifiedRelationships(step4a)
    step4c <- mapUnreifiedRelationships(step4b)
    // DataRelationships
    step5a <- mapEntityScalarDataProperties(step4c)
    step5b <- mapEntityStructuredDataProperties(step5a)
    step5c <- mapScalarDataProperties(step5b)
    step5d <- mapStructuredDataProperties(step5c)
    // Axioms
    step6a <- mapScalarOneOfLiteralAxioms(step5d)
    // - TermAxioms
    // -- EntityRestrictionAxioms
    step7a <- mapEntityExistentialRestrictionAxioms(step6a)
    step7b <- mapEntityUniversalRestrictionAxioms(step7a)
    // -- EntityScalarDataPropertyRestrictionAxioms
    step8a <- mapEntityScalarDataPropertyExistentialRestrictionAxioms(step7b)
    step8b <- mapEntityScalarDataPropertyParticularRestrictionAxioms(step8a)
    step8c <- mapEntityScalarDataPropertyUniversalRestrictionAxioms(step8b)
    // -- SpecializationAxiom
    step9a <- mapAspectSpecializationAxioms(step8c)
    step9b <- mapConceptSpecializationAxioms(step9a)
    step9c <- mapReifiedRelationshipSpecializationAxioms(step9b)
    // TerminologyBundleStatements
    step10a <- mapRootConceptTaxonomyAxioms(step9c)
    step10b <- mapAnonymousConceptTaxonomyAxioms(step10a)
    step10c <- mapSpecificDisjointConceptAxioms(step10b)
    // AnnotationPairs
    step11 <- mapAnnotationPairs(step10c)
  } yield {
    val c = step11.context
    val e1 =
      factory
        .copyExtent_modules(
          c.extent,
          c.graphs.foldLeft[Map[java.util.UUID, resolver.api.TerminologyGraph]](Map.empty[java.util.UUID, resolver.api.TerminologyGraph]) {
            case (acc, (uuid, g)) =>
              acc + (uuid -> g)
          })
    val e2 =
      factory
      .copyExtent_modules(
        c.extent,
        c.bundles.foldLeft[Map[java.util.UUID, resolver.api.Bundle]](Map.empty[java.util.UUID, resolver.api.Bundle]) {
          case (acc, (uuid, b)) =>
            acc + (uuid -> b)
        })

    step11.copy(context = c.copy(extent = e2))
  }


  def mapTerminologyGraphs
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    implicit val e: api.Extent = r.context.extent
    val gN = r.queue.terminologyGraphs.foldLeft(r.context.g) { (gi, t) =>
      val tg = r.factory.createTerminologyGraph(
        Some(r.context.extent.uuid),
        t.kind, t.iri,
        annotations=TreeSet.empty[api.Annotation],
        boxStatements=TreeSet.empty[api.TerminologyBoxStatement],
        boxAxioms=TreeSet.empty[api.TerminologyBoxAxiom])
      if (!OMLOps.uuidEquivalent(tg.uuid(r.context.extent), t.uuid))
        throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.TerminologyGraph UUID mismatch: read: $t; created: $tg")
      gi + tg
    }

    val t = r.copy(
      context = r.context.copy(g = gN),
      queue = r.queue.copy(terminologyGraphs = Seq()))
    Success(t)
  }

  def mapBundles
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    implicit val e: api.Extent = r.context.extent
    val gN = r.queue.bundles.foldLeft(r.context.g) { (gi, b) =>
      val tb = r.factory.createBundle(
        Some(r.context.extent.uuid),
        b.kind, b.iri,
        annotations=TreeSet.empty[api.Annotation],
        boxStatements=TreeSet.empty[api.TerminologyBoxStatement],
        boxAxioms=TreeSet.empty[api.TerminologyBoxAxiom],
        bundleStatements=TreeSet.empty[api.TerminologyBundleStatement],
        bundleAxioms=TreeSet.empty[api.TerminologyBundleAxiom])
      if (!OMLOps.uuidEquivalent(tb.uuid(r.context.extent), b.uuid))
        throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.Bundle UUID mismatch: read: $b; created: $tb")
      gi + tb
    }

    val t = r.copy(
      context = r.context.copy(g = gN),
      queue = r.queue.copy(bundles = Seq()))
    Success(t)
  }

  def seqopAppend[T]
  (s: Seq[T], entry: (UUID, ParSeq[T]))
  : Seq[T]
  = s ++ entry._2

  def seqopAppend1[T]
  (s: Seq[T], entry: ((UUID, UUID), T))
  : Seq[T]
  = s :+ entry._2

  def combopGraphs
  (g1: Graph[api.Module, TerminologyEdge],
   g2: Graph[api.Module, TerminologyEdge])
  : Graph[api.Module, TerminologyEdge]
  = g1.union(g2)

  type HyperGraphV = Try[Graph[api.Module, TerminologyEdge]]

  def seqopAspects
  (r: OMLTablesResolver)
  (h: HyperGraphV, entry: (UUID, Seq[tables.Aspect]))
  : HyperGraphV
  = h.flatMap { g =>
    g
      .toOuterNodes
      .find(_.uuid(r.context.extent).contains(entry._1))
      .fold[HyperGraphV](
      Failure(new java.lang.Error(
        s"OMFSchemaResolver.seqopAspects: there should be a graph with UUID=${entry._1}"))
    ) {
      case n0: api.TerminologyBox =>
        implicit val ex: api.Extent = r.context.extent
        val s = entry
          ._2
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, e) =>
            val ae = r.factory.createAspect(
              tbox = n0.uuid(r.context.extent),
              name = e.name)
            if (!OMLOps.uuidEquivalent(ae.uuid(r.context.extent), e.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.Aspect UUID mismatch: read: $e; created: $ae")
            acc + ae
        }

        val result = resolver.TerminologyContext.replaceNode(r, g, n0, n0.withBoxStatements(s))
        result
    }
  }

  def mapAspects
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val byUUID =
      r.queue.aspects
        .groupBy(_.tboxUUID)
        .map { case (uuid, aspects) => UUID.fromString(uuid) -> aspects }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    resolvable
      .foldLeft[HyperGraphV](Success(r.context.g))(seqopAspects(r))
      .map { g =>
        r.copy(
          context = TerminologyContext(r.context.extent, g),
          queue = r.queue.copy(aspects = unresolvable.flatMap(_._2).to[Seq]))
      }
  }

  def seqopConcepts
  (r: OMLTablesResolver)
  (h: HyperGraphV, entry: (UUID, Seq[tables.Concept]))
  : HyperGraphV
  = h.flatMap { g =>
    g
      .toOuterNodes
      .find(_.uuid(r.context.extent).contains(entry._1))
      .fold[HyperGraphV](
      Failure(new java.lang.Error(
        s"OMFSchemaResolver.seqopConcepts: there should be a graph with UUID=${entry._1}"))
    ) {
      case n0: api.TerminologyBox =>
        implicit val ex: api.Extent = r.context.extent
        val s =
          entry
            ._2
            .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
            case (acc, e) =>
              val c = r.factory.createConcept(
                tbox = n0.uuid(r.context.extent),
                name = e.name)
              if (!OMLOps.uuidEquivalent(c.uuid(r.context.extent), e.uuid))
                throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.concepts UUID mismatch; read: $e; got $c")
              acc + c
          }

        val result = TerminologyContext.replaceNode(r, g, n0, n0.withBoxStatements(s))
        result
    }
  }

  def mapConcepts
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val byUUID =
      r.queue.concepts
        .groupBy(_.tboxUUID)
        .map { case (uuid, concepts) => UUID.fromString(uuid) -> concepts }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    resolvable
      .foldLeft[HyperGraphV](Success(r.context.g))(seqopConcepts(r))
      .map { g =>
        r.copy(
            context = TerminologyContext(r.context.extent, g),
            queue = r.queue.copy(concepts = unresolvable.flatMap(_._2).to[Seq]))
      }
  }

  def seqopScalars
  (r: OMLTablesResolver)
  (h: HyperGraphV, entry: (UUID, Seq[tables.Scalar]))
  : HyperGraphV
  = h.flatMap { g =>
    g
      .toOuterNodes
      .find(_.uuid(r.context.extent).contains(entry._1))
      .fold[HyperGraphV](
      Failure(new java.lang.Error(
        s"OMFSchemaResolver.seqopScalars: there should be a graph with UUID=${entry._1}"))
    ) {
      case n0: api.TerminologyBox =>
        implicit val ex: api.Extent = r.context.extent
        val s = entry
          ._2
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, e) =>
            val x =
              r.factory.createScalar(
                tbox = n0.uuid(r.context.extent),
                name = e.name)
            if (!OMLOps.uuidEquivalent(x.uuid(r.context.extent), e.uuid))
              throw new java.lang.IllegalArgumentException(s"DataRangteResolver.Scalar UUID mismatch: read: $e, created: $x")
            acc + x
        }

        val result = TerminologyContext.replaceNode(r, g, n0, n0.withBoxStatements(s))
        result
    }
  }

  def mapScalars
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val byUUID =
      r.queue.scalars
        .groupBy(_.tboxUUID)
        .map { case (uuid, scalars) => UUID.fromString(uuid) -> scalars }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    resolvable
      .foldLeft[HyperGraphV](Success(r.context.g))(seqopScalars(r))
      .map { g =>
        r.copy(
            context = TerminologyContext(r.context.extent, g),
            queue = r.queue.copy(scalars = unresolvable.flatMap(_._2).to[Seq]))
      }
  }

  def seqopStructures
  (r: OMLTablesResolver)
  (h: HyperGraphV, entry: (UUID, Seq[tables.Structure]))
  : HyperGraphV
  = h.flatMap { g =>
    g
      .toOuterNodes
      .find(_.uuid(r.context.extent).contains(entry._1))
      .fold[HyperGraphV](
      Failure(new java.lang.Error(
        s"OMFSchemaResolver.seqopStructures: there should be a graph with UUID=${entry._1}"))
    ) {
      case n0: api.TerminologyBox =>
        implicit val ex: api.Extent = r.context.extent
        val s = entry
          ._2
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, e) =>
            val x =
              r.factory.createStructure(
                tbox = n0.uuid(r.context.extent),
                name = e.name)
            if (!OMLOps.uuidEquivalent(x.uuid(r.context.extent), e.uuid))
              throw new java.lang.IllegalArgumentException(s"DataRangteResolver.Structure UUID mismatch: read: $e, created: $x")
            acc + x
        }

        val result = TerminologyContext.replaceNode(r, g, n0, n0.withBoxStatements(s))
        result
    }
  }

  def mapStructures
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val byUUID =
      r.queue.structures
        .groupBy(_.tboxUUID)
        .map { case (uuid, structures) => UUID.fromString(uuid) -> structures }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    resolvable
      .foldLeft[HyperGraphV](Success(r.context.g))(seqopStructures(r))
      .map { g =>
        r.copy(
            context = TerminologyContext(r.context.extent, g),
            queue = r.queue.copy(structures = unresolvable.flatMap(_._2).to[Seq]))
      }
  }

  def seqopTerminologyExtends
  (r: OMLTablesResolver,
   nodes: Map[UUID, api.TerminologyBox])
  (g: Graph[api.Module, TerminologyEdge],
   entry: ((UUID, UUID), tables.TerminologyExtensionAxiom))
  : Graph[api.Module, TerminologyEdge]
  = {
    val extending = nodes(entry._1._1)
    val extended = nodes(entry._1._2)

      val x = r.factory.createTerminologyExtensionAxiom(
        tbox=extending.uuid(r.context.extent),
        extendedTerminology=extended)

    if (!OMLOps.uuidEquivalent(x.uuid(r.context.extent), entry._2.uuid))
      throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.TerminologyExtends UUID mismatch: read: ${entry._2}, created: $x")
    val result = g + resolver.TerminologyEdge(extending, extended, x)

    result
  }

  def mapTerminologyExtends
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val byUUID =
      r.queue.terminologyExtensionAxioms.par
        .map { tAxiom =>
          (UUID.fromString(tAxiom.tboxUUID), UUID.fromString(tAxiom.extendedTerminologyUUID)) -> tAxiom
        }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUIDPair, _) =>
          ns.contains(tboxUUIDPair._1) && ns.contains(tboxUUIDPair._2)
        }

    val g = resolvable.aggregate(r.context.g)(seqopTerminologyExtends(r,ns), combopGraphs)

    val s = unresolvable.map(_._2).seq

    val t =
      r
        .copy(
          context = TerminologyContext(r.context.extent, g),
          queue = r.queue.copy(terminologyExtensionAxioms = s))

    Success(t)
  }

  def seqopTerminologyNesting
  (r: OMLTablesResolver,
   ns: Map[tables.UUID, api.TerminologyGraph],
   cs: Map[tables.UUID, (api.Concept, api.TerminologyBox)])
  (g: Graph[api.Module, TerminologyEdge],
   entry: ((tables.UUID, tables.UUID), tables.TerminologyNestingAxiom))
  : Graph[api.Module, TerminologyEdge]
  = {
    val nestedT = ns(entry._1._1)
    val (nestingC, nestingT) = cs(entry._1._2)

    val x = r.factory.createTerminologyNestingAxiom(
        tbox=nestedT.uuid(r.context.extent),
        nestingTerminology=nestingT,
        nestingContext=nestingC)

    if (!OMLOps.uuidEquivalent(x.uuid(r.context.extent), entry._2.uuid))
      throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.TerminologyNestingAxiom UUID mismatch: read: ${entry._2}, created: $x")
    val result = g + resolver.TerminologyEdge(nestedT, nestingT, x)
    result
  }

  def mapTerminologyNestings
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    import OMLOps._
    implicit val ex: api.Extent = r.context.extent

    val ns
    : Map[tables.UUID, api.TerminologyGraph]
    = r.context.graphs.map {
      case (uuid, g: api.TerminologyGraph) =>
        uuid.toString -> g
    }

    val referencableConcepts
    : Map[tables.UUID, (api.Concept, api.TerminologyBox)]
    = r.context.g.nodes.toOuter
      .foldLeft[Map[tables.UUID, (api.Concept, api.TerminologyBox)]](Map.empty) { case (acc, t: api.TerminologyBox) =>
      acc ++
      t.concepts.map { c =>
        c.uuid(r.context.extent).toString -> (c -> t)
      }
    }

    val byUUID
    : Seq[((tables.UUID, tables.UUID), tables.TerminologyNestingAxiom)]
    = r.queue.terminologyNestingAxioms
      .map { tAxiom =>
        (tAxiom.tboxUUID -> tAxiom.nestingContextUUID) -> tAxiom
      }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUIDPair, tx) =>
          ns.contains(tboxUUIDPair._1) &&
            referencableConcepts.get(tboxUUIDPair._2).fold[Boolean](false){ case (c,tbox) =>
                tbox.uuid(r.context.extent).contains(UUID.fromString(tx.nestingTerminologyUUID))
            }
        }

    val g =
      resolvable
        .aggregate(r.context.g)(seqopTerminologyNesting(r, ns, referencableConcepts), combopGraphs)

    val s =
      unresolvable.map(_._2).seq

    val t =
      r
        .copy(
          context = TerminologyContext(r.context.extent, g),
          queue = r.queue.copy(terminologyNestingAxioms = s))

    Success(t)
  }

  def seqopConceptDesignationTerminology
  (r: OMLTablesResolver,
   ns: Map[tables.UUID, api.TerminologyGraph],
   cs: Map[tables.UUID, (api.Concept, api.TerminologyBox)])
  (g: Graph[api.Module, TerminologyEdge],
   entry: ((tables.UUID, tables.UUID), tables.ConceptDesignationTerminologyAxiom))
  : Graph[api.Module, TerminologyEdge]
  = {
    val designationG = ns(entry._1._1)
    val (designatedC, designatedTBox) = cs(entry._1._2)
    val x = r.factory.createConceptDesignationTerminologyAxiom(
        tbox=designationG.uuid(r.context.extent),
        designatedConcept=designatedC,
        designatedTerminology=designatedTBox)

    if (!OMLOps.uuidEquivalent(x.uuid(r.context.extent), entry._2.uuid))
      throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ConceptDesignationTerminologyAxiom UUID mismatch: read: ${entry._2}, created: $x")
    val result = g + resolver.TerminologyEdge(designationG, designatedTBox, x)

    result
  }

  def mapConceptDesignationTerminologyAxioms
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    import OMLOps._
    implicit val ex: api.Extent = r.context.extent

    val ns
    : Map[tables.UUID, api.TerminologyGraph]
    = r.context.tboxes.flatMap {
      case (uuid, g: api.TerminologyGraph) =>
        Some(uuid.toString -> g)
      case _ =>
        None
    }

    val referencableConcepts
    : Map[tables.UUID, (api.Concept, api.TerminologyBox)]
    = r.context.g.nodes.toOuter
      .foldLeft[Map[tables.UUID, (api.Concept, api.TerminologyBox)]](Map.empty) { case (acc, t: api.TerminologyBox) =>
      acc ++
        t.concepts.map { c =>
          c.uuid(r.context.extent).toString -> (c -> t)
        }
    }

    val byUUID
    : Seq[((tables.UUID, tables.UUID), tables.ConceptDesignationTerminologyAxiom)]
    = r.queue.conceptDesignationTerminologyAxioms
      .map { tAxiom =>
        (tAxiom.tboxUUID -> tAxiom.designatedConceptUUID) -> tAxiom
      }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUIDPair, _) =>
          ns.contains(tboxUUIDPair._1) &&
            referencableConcepts.contains(tboxUUIDPair._2)
        }

    val g =
      resolvable
        .aggregate(r.context.g)(
          seqopConceptDesignationTerminology(r, ns, referencableConcepts),
          combopGraphs)

    val s =
      unresolvable.map(_._2).seq

    val t =
      r
        .copy(
          context = TerminologyContext(r.context.extent, g),
          queue = r.queue.copy(conceptDesignationTerminologyAxioms = s))

    Success(t)
  }

  def seqopBundledTerminologyAxioms
  (r: OMLTablesResolver,
   bundles: Map[UUID, api.Bundle],
   nodes: Map[UUID, api.TerminologyBox])
  (g: Graph[api.Module, TerminologyEdge],
   entry: ((UUID, UUID), tables.BundledTerminologyAxiom))
  : Graph[api.Module, TerminologyEdge]
  = {
    val bundling = bundles(entry._1._1)
    val bundled = nodes(entry._1._2)

    val x = r.factory.createBundledTerminologyAxiom(
        bundle = bundling.uuid(r.context.extent),
        bundledTerminology = bundled)

    if (!OMLOps.uuidEquivalent(x.uuid(r.context.extent), entry._2.uuid))
      throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.BundledTerminologyAxiom UUID mismatch: read: ${entry._2}, created: $x")
    val result = g + resolver.TerminologyEdge(bundling, bundled, x)

    result
  }

  def mapBundledTerminologyAxioms
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val bs = r.context.bundles
    val ns = r.context.tboxes
    val byUUID =
      r.queue.bundledTerminologyAxioms.par
        .flatMap { tAxiom =>
          tAxiom.uuid.map(id => (UUID.fromString(id), UUID.fromString(tAxiom.bundledTerminologyUUID)) -> tAxiom)
        }

    val (resolvable, unresolvable) =
      byUUID
        .partition { case (tboxUUIDPair, _) =>
          bs.contains(tboxUUIDPair._1) && ns.contains(tboxUUIDPair._2)
        }

    val g = resolvable.aggregate(r.context.g)(
      seqopBundledTerminologyAxioms(r, bs, ns),
      combopGraphs)

    val s = unresolvable.map(_._2).seq

    val t =
      r
        .copy(
          context = TerminologyContext(r.context.extent, g),
          queue = r.queue.copy(bundledTerminologyAxioms = s))

    Success(t)
  }

  def mapRestrictedDataRanges
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val g = r.context.g
    val (r1, u1) =
      r.queue.binaryScalarRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }
    val (r2, u2) =
      r.queue.iriScalarRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }
    val (r3, u3) =
      r.queue.numericScalarRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }
    val (r4, u4) =
      r.queue.plainLiteralScalarRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }
    val (r5, u5) =
      r.queue.scalarOneOfRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }
    val (r6, u6) =
      r.queue.stringScalarRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }
    val (r7, u7) =
      r.queue.timeScalarRestrictions
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val worklist = DataRangesToResolve(
      binaryScalarRestrictions =
        r1.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) },
      iriScalarRestrictions =
        r2.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) },
      numericScalarRestrictions =
        r3.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) },
      plainLiteralScalarRestrictions =
        r4.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) },
      scalarOneOfRestrictions =
        r5.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) },
      stringScalarRestrictions =
        r6.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) },
      timeScalarRestrictions =
        r7.map { case (guuid, drs) => guuid -> drs.map(dr => UUID.fromString(dr.restrictedRangeUUID) -> dr) }
    )

    DataRangesToResolve
      .resolve(r, worklist)
      .map { case (resolved, remaining) =>

        val updated = resolved.queue.copy(
          binaryScalarRestrictions =
            u1.flatMap(_._2).to[Seq] ++ remaining.binaryScalarRestrictions.flatMap(_._2.map(_._2)).to[Seq],
          iriScalarRestrictions =
            u2.flatMap(_._2).to[Seq] ++ remaining.iriScalarRestrictions.flatMap(_._2.map(_._2)).to[Seq],
          numericScalarRestrictions =
            u3.flatMap(_._2).to[Seq] ++ remaining.numericScalarRestrictions.flatMap(_._2.map(_._2)).to[Seq],
          plainLiteralScalarRestrictions =
            u4.flatMap(_._2).to[Seq] ++ remaining.plainLiteralScalarRestrictions.flatMap(_._2.map(_._2)).to[Seq],
          scalarOneOfRestrictions =
            u5.flatMap(_._2).to[Seq] ++ remaining.scalarOneOfRestrictions.flatMap(_._2.map(_._2)).to[Seq],
          stringScalarRestrictions =
            u6.flatMap(_._2).to[Seq] ++ remaining.stringScalarRestrictions.flatMap(_._2.map(_._2)).to[Seq],
          timeScalarRestrictions =
            u7.flatMap(_._2).to[Seq] ++ remaining.timeScalarRestrictions.flatMap(_._2.map(_._2)).to[Seq]
        )

        resolved.copy(queue = updated)
      }
  }

  def mapReifiedRelationships
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val g = r.context.g
    val (resolvable, unresolved) =
      r.queue.reifiedRelationships
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = r.copy(queue = r.queue.copy(reifiedRelationships = unresolved.flatMap(_._2).to[Seq]))
    resolveReifiedRelationships(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(reifiedRelationships = r2.queue.reifiedRelationships ++ remaining))
    }
  }

  final def resolveReifiedRelationships
  (r: OMLTablesResolver, queue: Map[UUID, Seq[tables.ReifiedRelationship]])
  : Try[(OMLTablesResolver, Seq[tables.ReifiedRelationship])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.ReifiedRelationship]], Boolean)]](
      Success(Tuple3(r, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = r.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { rr =>
          referencableEntities.contains(rr.sourceUUID) && referencableEntities.contains(rr.targetUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, rr) =>
            val x =
              ri.factory.createReifiedRelationship(
                tbox.uuid(ri.context.extent),
                referencableEntities(rr.sourceUUID),
                referencableEntities(rr.targetUUID),
                rr.isAsymmetric,
                rr.isEssential,
                rr.isFunctional,
                rr.isInverseEssential,
                rr.isInverseFunctional,
                rr.isIrreflexive,
                rr.isReflexive,
                rr.isSymmetric,
                rr.isTransitive,
                rr.name,
                rr.unreifiedPropertyName,
                rr.unreifiedInversePropertyName)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), rr.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ReifiedRelationship UUID mismatch: read: $rr, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(rr, m, f)) =>
        if (f)
          resolveReifiedRelationships(rr, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapUnreifiedRelationships
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val g = r.context.g
    val (resolvable, unresolved) =
      r.queue.unreifiedRelationships
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = r.copy(queue = r.queue.copy(unreifiedRelationships = unresolved.flatMap(_._2).to[Seq]))
    resolveUnreifiedRelationships(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(unreifiedRelationships = r2.queue.unreifiedRelationships ++ remaining))
    }
  }

  final def resolveUnreifiedRelationships
  (r: OMLTablesResolver, queue: Map[UUID, Seq[tables.UnreifiedRelationship]] )
  : Try[(OMLTablesResolver, Seq[tables.UnreifiedRelationship])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.UnreifiedRelationship]], Boolean)]](
      Success(Tuple3(r, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ur =>
          referencableEntities.contains(ur.sourceUUID) && referencableEntities.contains(ur.targetUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ur) =>
            val x =
              ri.factory.createUnreifiedRelationship(
                tbox.uuid(ex),
                referencableEntities(ur.sourceUUID),
                referencableEntities(ur.targetUUID),
                ur.isAsymmetric,
                ur.isEssential,
                ur.isFunctional,
                ur.isInverseEssential,
                ur.isInverseFunctional,
                ur.isIrreflexive,
                ur.isReflexive,
                ur.isSymmetric,
                ur.isTransitive,
                ur.name)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ur.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ReifiedRelationship UUID mismatch: read: $ur, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(ur, m, f)) =>
        if (f)
          resolveUnreifiedRelationships(ur, m)
        else
          Success(Tuple2(ur, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityScalarDataProperties
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = r.context.tboxes
    val g = r.context.g
    val (resolvable, unresolved) =
      r.queue.entityScalarDataProperties
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = r.copy(queue = r.queue.copy(entityScalarDataProperties = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityScalarDataProperties(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityScalarDataProperties = r2.queue.entityScalarDataProperties ++ remaining))
    }
  }

  final def resolveEntityScalarDataProperties
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityScalarDataProperty]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityScalarDataProperty])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityScalarDataProperty]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableDataRanges
        : Map[tables.UUID, api.DataRange]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.DataRange]](Map.empty)(_ ++ _.dataranges.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val (available, remaining) = x.partition { dr =>
          referencableEntities.contains(dr.domainUUID) &&
            referencableDataRanges.contains(dr.rangeUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, dr) =>
            val x =
              ri.factory.createEntityScalarDataProperty(
                tbox.uuid(ex),
                referencableEntities(dr.domainUUID),
                referencableDataRanges(dr.rangeUUID),
                dr.isIdentityCriteria,
                dr.name)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), dr.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.UnreifiedRelationship UUID mismatch: read: $dr, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityScalarDataProperties(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityStructuredDataProperties
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.entityStructuredDataProperties
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(entityStructuredDataProperties = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityStructuredDataProperties(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityStructuredDataProperties = r2.queue.entityStructuredDataProperties ++ remaining))
    }
  }

  final def resolveEntityStructuredDataProperties
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityStructuredDataProperty]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityStructuredDataProperty])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityStructuredDataProperty]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableStructures
        : Map[tables.UUID, api.Structure]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Structure]](Map.empty)(_ ++ _.structures.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val (available, remaining) = x.partition { dr =>
          referencableEntities.contains(dr.domainUUID) &&
            referencableStructures.contains(dr.rangeUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, dr) =>
            val x =
              ri.factory.createEntityStructuredDataProperty(
                tbox.uuid(ex),
                referencableEntities(dr.domainUUID),
                referencableStructures(dr.rangeUUID),
                dr.isIdentityCriteria,
                dr.name)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), dr.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.EntityStructuredDataProperty UUID mismatch: read: $dr, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityStructuredDataProperties(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapScalarDataProperties
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.scalarDataProperties
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(scalarDataProperties = unresolved.flatMap(_._2).to[Seq]))
    resolveScalarDataProperties(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(scalarDataProperties = r2.queue.scalarDataProperties ++ remaining))
    }
  }

  final def resolveScalarDataProperties
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.ScalarDataProperty]] )
  : Try[(OMLTablesResolver, Seq[tables.ScalarDataProperty])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.ScalarDataProperty]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableStructures
        : Map[tables.UUID, api.Structure]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Structure]](Map.empty)(_ ++ _.structures.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val referencableDataRanges
        : Map[tables.UUID, api.DataRange]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.DataRange]](Map.empty)(_ ++ _.dataranges.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val (available, remaining) = x.partition { dr =>
          referencableStructures.contains(dr.domainUUID) &&
            referencableDataRanges.contains(dr.rangeUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, dr) =>
            val x =
              ri.factory.createScalarDataProperty(
                tbox.uuid(ex),
                referencableStructures(dr.domainUUID),
                referencableDataRanges(dr.rangeUUID),
                dr.name)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), dr.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ScalarDataProperty UUID mismatch: read: $dr, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveScalarDataProperties(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapStructuredDataProperties
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.structuredDataProperties
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(structuredDataProperties = unresolved.flatMap(_._2).to[Seq]))
    resolveStructuredDataProperties(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(structuredDataProperties = r2.queue.structuredDataProperties ++ remaining))
    }
  }

  final def resolveStructuredDataProperties
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.StructuredDataProperty]] )
  : Try[(OMLTablesResolver, Seq[tables.StructuredDataProperty])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.StructuredDataProperty]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableStructures
        : Map[tables.UUID, api.Structure]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Structure]](Map.empty)(_ ++ _.structures.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val (available, remaining) = x.partition { dr =>
          referencableStructures.contains(dr.domainUUID) &&
            referencableStructures.contains(dr.rangeUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, dr) =>
            val x =
            ri.factory.createStructuredDataProperty(
              tbox.uuid(ex),
              referencableStructures(dr.domainUUID),
              referencableStructures(dr.rangeUUID),
              dr.name)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), dr.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.StructuredDataProperty UUID mismatch: read: $dr, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveStructuredDataProperties(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapScalarOneOfLiteralAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.scalarOneOfLiteralAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(scalarOneOfLiteralAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveScalarOneOfLiteralAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(scalarOneOfLiteralAxioms = r2.queue.scalarOneOfLiteralAxioms ++ remaining))
    }
  }

  final def resolveScalarOneOfLiteralAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.ScalarOneOfLiteralAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.ScalarOneOfLiteralAxiom])]
  = {
    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.ScalarOneOfLiteralAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)
        val scalarOneOfRestrictions =
          gi
            .outerNodeTraverser(gi.get(tbox))
            .flatMap {
              case t: api.TerminologyBox =>
                t.boxStatements.flatMap {
                  case ax: api.ScalarOneOfRestriction =>
                    ax.uuid(ex).map(id => id -> ax)
                  case _ =>
                    None
                }
            }
            .toMap
        val (available, remaining) = x.partition { ax =>
          scalarOneOfRestrictions.contains(UUID.fromString(ax.axiomUUID))
        }
        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createScalarOneOfLiteralAxiom(
                tbox.uuid(ex),
                scalarOneOfRestrictions(UUID.fromString(ax.axiomUUID)),
                ax.value)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ScalarOneOfLiteralAxiom UUID mismatch: read: $ax, created: $x")

            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveScalarOneOfLiteralAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityExistentialRestrictionAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.entityExistentialRestrictionAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(entityExistentialRestrictionAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityExistentialRestrictionAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityExistentialRestrictionAxioms = r2.queue.entityExistentialRestrictionAxioms ++ remaining))
    }
  }

  final def resolveEntityExistentialRestrictionAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityExistentialRestrictionAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityExistentialRestrictionAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityExistentialRestrictionAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableReifiedRelationships
        : Map[tables.UUID, api.ReifiedRelationship]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.ReifiedRelationship]](Map.empty)(_ ++ _.reifiedRelationships.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ax =>
          referencableEntities.contains(ax.restrictedDomainUUID) &&
            referencableEntities.contains(ax.restrictedRangeUUID) &&
            referencableReifiedRelationships.contains(ax.restrictedRelationUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
            ri.factory.createEntityExistentialRestrictionAxiom(
              tbox.uuid(ex),
              referencableReifiedRelationships(ax.restrictedRelationUUID),
              referencableEntities(ax.restrictedDomainUUID),
              referencableEntities(ax.restrictedRangeUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ExistentialRestrictionAxiom UUID mismatch: read: $ax created: $x")

            acc + x
          }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityExistentialRestrictionAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityUniversalRestrictionAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.entityUniversalRestrictionAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(entityUniversalRestrictionAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityUniversalRestrictionAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityUniversalRestrictionAxioms = r2.queue.entityUniversalRestrictionAxioms ++ remaining))
    }
  }

  final def resolveEntityUniversalRestrictionAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityUniversalRestrictionAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityUniversalRestrictionAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityUniversalRestrictionAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableReifiedRelationships
        : Map[tables.UUID, api.ReifiedRelationship]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.ReifiedRelationship]](Map.empty)(_ ++ _.reifiedRelationships.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ax =>
          referencableEntities.contains(ax.restrictedDomainUUID) &&
            referencableEntities.contains(ax.restrictedRangeUUID) &&
            referencableReifiedRelationships.contains(ax.restrictedRelationUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
            ri.factory.createEntityUniversalRestrictionAxiom(
              tbox.uuid(ex),
              referencableReifiedRelationships(ax.restrictedRelationUUID),
              referencableEntities(ax.restrictedDomainUUID),
              referencableEntities(ax.restrictedRangeUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.EntityUniversalRestrictionAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityUniversalRestrictionAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityScalarDataPropertyExistentialRestrictionAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.entityScalarDataPropertyExistentialRestrictionAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(entityScalarDataPropertyExistentialRestrictionAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityScalarDataPropertyExistentialRestrictionAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityScalarDataPropertyExistentialRestrictionAxioms = r2.queue.entityScalarDataPropertyExistentialRestrictionAxioms ++ remaining))
    }
  }

  final def resolveEntityScalarDataPropertyExistentialRestrictionAxioms
  (r: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityScalarDataPropertyExistentialRestrictionAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityScalarDataPropertyExistentialRestrictionAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityScalarDataPropertyExistentialRestrictionAxiom]], Boolean)]](
      Success(Tuple3(r, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableEntityScalarDataProperty
        : Map[tables.UUID, api.EntityScalarDataProperty]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.EntityScalarDataProperty]](Map.empty)(_ ++ _.entityScalarDataProperties.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val referencableDataRanges
        : Map[tables.UUID, api.DataRange]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.DataRange]](Map.empty)(_ ++ _.dataranges.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val (available, remaining) = x.partition { ax =>
          referencableEntities.contains(ax.restrictedEntityUUID) &&
            referencableEntityScalarDataProperty.contains(ax.scalarPropertyUUID) &&
            referencableDataRanges.contains(ax.scalarRestrictionUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createEntityScalarDataPropertyExistentialRestrictionAxiom(
                tbox.uuid(ex),
                referencableEntities(ax.restrictedEntityUUID),
                referencableEntityScalarDataProperty(ax.scalarPropertyUUID),
                referencableDataRanges(ax.scalarRestrictionUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.EntityExistentialRestrictionAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityScalarDataPropertyExistentialRestrictionAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityScalarDataPropertyParticularRestrictionAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.entityScalarDataPropertyParticularRestrictionAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(entityScalarDataPropertyParticularRestrictionAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityScalarDataPropertyParticularRestrictionAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityScalarDataPropertyParticularRestrictionAxioms = r2.queue.entityScalarDataPropertyParticularRestrictionAxioms ++ remaining))
    }
  }

  final def resolveEntityScalarDataPropertyParticularRestrictionAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityScalarDataPropertyParticularRestrictionAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityScalarDataPropertyParticularRestrictionAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityScalarDataPropertyParticularRestrictionAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableEntityScalarDataProperty
        : Map[tables.UUID, api.EntityScalarDataProperty]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.EntityScalarDataProperty]](Map.empty)(_ ++ _.entityScalarDataProperties.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))


        val (available, remaining) = x.partition { ax =>
          referencableEntities.contains(ax.restrictedEntityUUID) &&
            referencableEntityScalarDataProperty.contains(ax.scalarPropertyUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
            ri.factory.createEntityScalarDataPropertyParticularRestrictionAxiom(
              tbox.uuid(ex),
              referencableEntities(ax.restrictedEntityUUID),
              referencableEntityScalarDataProperty(ax.scalarPropertyUUID),
              ax.literalValue)

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.EntityScalarDataPropertyParticularRestrictionAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityScalarDataPropertyParticularRestrictionAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapEntityScalarDataPropertyUniversalRestrictionAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.entityScalarDataPropertyUniversalRestrictionAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(entityScalarDataPropertyUniversalRestrictionAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveEntityScalarDataPropertyUniversalRestrictionAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(entityScalarDataPropertyUniversalRestrictionAxioms = r2.queue.entityScalarDataPropertyUniversalRestrictionAxioms ++ remaining))
    }
  }

  final def resolveEntityScalarDataPropertyUniversalRestrictionAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.EntityScalarDataPropertyUniversalRestrictionAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.EntityScalarDataPropertyUniversalRestrictionAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.EntityScalarDataPropertyUniversalRestrictionAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableEntityScalarDataProperty
        : Map[tables.UUID, api.EntityScalarDataProperty]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.EntityScalarDataProperty]](Map.empty)(_ ++ _.entityScalarDataProperties.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val referencableDataRanges
        : Map[tables.UUID, api.DataRange]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.DataRange]](Map.empty)(_ ++ _.dataranges.flatMap(dr => dr.uuid(ex).map(id => id.toString -> dr)))

        val (available, remaining) = x.partition { ax =>
          referencableEntities.contains(ax.restrictedEntityUUID) &&
            referencableEntityScalarDataProperty.contains(ax.scalarPropertyUUID) &&
            referencableDataRanges.contains(ax.scalarRestrictionUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
            ri.factory.createEntityScalarDataPropertyUniversalRestrictionAxiom(
              tbox.uuid(ex),
              referencableEntities(ax.restrictedEntityUUID),
              referencableEntityScalarDataProperty(ax.scalarPropertyUUID),
              referencableDataRanges(ax.scalarRestrictionUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.EntityScalarDataPropertyUniversalRestrictionAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveEntityScalarDataPropertyUniversalRestrictionAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapAspectSpecializationAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.aspectSpecializationAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(aspectSpecializationAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveAspectSpecializationAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(aspectSpecializationAxioms = r2.queue.aspectSpecializationAxioms ++ remaining))
    }
  }

  final def resolveAspectSpecializationAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.AspectSpecializationAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.AspectSpecializationAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.AspectSpecializationAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableEntities
        : Map[tables.UUID, api.Entity]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Entity]](Map.empty)(_ ++ _.entities.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableAspects
        : Map[tables.UUID, api.Aspect]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Aspect]](Map.empty)(_ ++ _.aspects.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ax =>
          referencableEntities.contains(ax.subEntityUUID) &&
            referencableAspects.contains(ax.superAspectUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createAspectSpecializationAxiom(
                tbox.uuid(ex),
                referencableAspects(ax.superAspectUUID),
                referencableEntities(ax.subEntityUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.AspectSpecializationAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveAspectSpecializationAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapConceptSpecializationAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.conceptSpecializationAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(conceptSpecializationAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveConceptSpecializationAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(conceptSpecializationAxioms = r2.queue.conceptSpecializationAxioms ++ remaining))
    }
  }

  final def resolveConceptSpecializationAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.ConceptSpecializationAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.ConceptSpecializationAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.ConceptSpecializationAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableConcepts
        : Map[tables.UUID, api.Concept]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.Concept]](Map.empty)(_ ++ _.concepts.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ax =>
          referencableConcepts.contains(ax.subConceptUUID) &&
            referencableConcepts.contains(ax.superConceptUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createConceptSpecializationAxiom(
                tbox.uuid(ex),
                referencableConcepts(ax.superConceptUUID),
                referencableConcepts(ax.subConceptUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ConceptSpecializationAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveConceptSpecializationAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapReifiedRelationshipSpecializationAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.tboxes
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.reifiedRelationshipSpecializationAxioms
        .groupBy(_.tboxUUID)
        .map { case (uuid, ranges) => UUID.fromString(uuid) -> ranges }
        .partition { case (tboxUUID, _) => ns.contains(tboxUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(reifiedRelationshipSpecializationAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveReifiedRelationshipSpecializationAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(reifiedRelationshipSpecializationAxioms = r2.queue.reifiedRelationshipSpecializationAxioms ++ remaining))
    }
  }

  final def resolveReifiedRelationshipSpecializationAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.ReifiedRelationshipSpecializationAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.ReifiedRelationshipSpecializationAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.ReifiedRelationshipSpecializationAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (guuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val tbox = ri.context.tboxes(guuid)

        val referencableReifiedRelationships
        : Map[tables.UUID, api.ReifiedRelationship]
        = gi.outerNodeTraverser(gi.get(tbox))
          .foldLeft[Map[tables.UUID, api.ReifiedRelationship]](Map.empty)(_ ++ _.reifiedRelationships.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ax =>
          referencableReifiedRelationships.contains(ax.subRelationshipUUID) &&
            referencableReifiedRelationships.contains(ax.superRelationshipUUID)
        }

        val si
        : SortedSet[api.TerminologyBoxStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBoxStatement]](TreeSet.empty[api.TerminologyBoxStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createReifiedRelationshipSpecializationAxiom(
                tbox.uuid(ex),
                referencableReifiedRelationships(ax.superRelationshipUUID),
                referencableReifiedRelationships(ax.subRelationshipUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.ReifiedRelationshipSpecializationAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, tbox, tbox.withBoxStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (guuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveReifiedRelationshipSpecializationAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapRootConceptTaxonomyAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.bundles
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.rootConceptTaxonomyAxioms
        .groupBy(_.bundleUUID)
        .map { case (uuid, axioms) => UUID.fromString(uuid) -> axioms }
        .partition { case (bundleUUID, _) => ns.contains(bundleUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(rootConceptTaxonomyAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveRootConceptTaxonomyAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(rootConceptTaxonomyAxioms = r2.queue.rootConceptTaxonomyAxioms ++ remaining))
    }
  }

  final def resolveRootConceptTaxonomyAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.RootConceptTaxonomyAxiom]] )
  : Try[(OMLTablesResolver, Seq[tables.RootConceptTaxonomyAxiom])]
  = {
    import OMLOps._

    queue.foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.RootConceptTaxonomyAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (buuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val bundle = ri.context.bundles(buuid)

        val referencableConcepts
        : Map[tables.UUID, api.Concept]
        = gi.outerNodeTraverser(gi.get(bundle))
          .foldLeft[Map[tables.UUID, api.Concept]](Map.empty)(_ ++ _.concepts.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val (available, remaining) = x.partition { ax =>
          referencableConcepts.contains(ax.rootUUID)
        }

        val si
        : SortedSet[api.TerminologyBundleStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBundleStatement]](TreeSet.empty[api.TerminologyBundleStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createRootConceptTaxonomyAxiom(
                bundle.uuid(ex),
                referencableConcepts(ax.rootUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.RootConceptTaxonomyAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, bundle, bundle.withBundleStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (buuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveRootConceptTaxonomyAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapAnonymousConceptTaxonomyAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.bundles
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.anonymousConceptTaxonomyAxioms
        .groupBy(_.bundleUUID)
        .map { case (uuid, axioms) => UUID.fromString(uuid) -> axioms }
        .partition { case (bundleUUID, _) => ns.contains(bundleUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(anonymousConceptTaxonomyAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveAnonymousConceptTaxonomyAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(anonymousConceptTaxonomyAxioms = r2.queue.anonymousConceptTaxonomyAxioms ++ remaining))
    }
  }

  final def resolveAnonymousConceptTaxonomyAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.AnonymousConceptTaxonomyAxiom]])
  : Try[(OMLTablesResolver, Seq[tables.AnonymousConceptTaxonomyAxiom])]
  = {
    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.AnonymousConceptTaxonomyAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (buuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val bundle = ri.context.bundles(buuid)

        val referencableConceptTreeDisjunctions
        : Map[tables.UUID, api.ConceptTreeDisjunction]
        = gi.outerNodeTraverser(gi.get(bundle))
          .flatMap {
            case b: api.Bundle => Some(b)
            case _ => None
          }
          .foldLeft[Map[tables.UUID, api.ConceptTreeDisjunction]](Map.empty)(
          _ ++
            _.bundleStatements.flatMap {
                case csd: api.TerminologyBundleStatement with api.ConceptTreeDisjunction =>
                  csd.uuid(ex).map(id => id.toString -> csd)
                case _ =>
                  None
              }
              .toMap)

        val (available, remaining) = x.partition { ax =>
          referencableConceptTreeDisjunctions.contains(ax.disjointTaxonomyParentUUID)
        }

        val si
        : SortedSet[api.TerminologyBundleStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBundleStatement]](TreeSet.empty[api.TerminologyBundleStatement]) {
          case (acc, ax) =>
            val x =
            ri.factory.createAnonymousConceptTaxonomyAxiom(
              bundle.uuid(ex),
              referencableConceptTreeDisjunctions(ax.disjointTaxonomyParentUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.AnonymousConceptTaxonomyAxiom UUID mismatch: read: $ax, created: $x")
            acc + x
          }

        TerminologyContext
          .replaceNode(ri, gi, bundle, bundle.withBundleStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (buuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveAnonymousConceptTaxonomyAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  def mapSpecificDisjointConceptAxioms
  (resolver: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    val ns = resolver.context.bundles
    val g = resolver.context.g
    val (resolvable, unresolved) =
      resolver.queue.specificDisjointConceptAxioms
        .groupBy(_.bundleUUID)
        .map { case (uuid, axioms) => UUID.fromString(uuid) -> axioms }
        .partition { case (bundleUUID, _) => ns.contains(bundleUUID) }

    val r1 = resolver.copy(queue = resolver.queue.copy(specificDisjointConceptAxioms = unresolved.flatMap(_._2).to[Seq]))
    resolveSpecificDisjointConceptAxioms(r1, resolvable).map {
      case (r2, remaining) =>
        r2.copy(queue = r2.queue.copy(specificDisjointConceptAxioms = r2.queue.specificDisjointConceptAxioms ++ remaining))
    }
  }

  final def resolveSpecificDisjointConceptAxioms
  (resolver: OMLTablesResolver, queue: Map[UUID, Seq[tables.SpecificDisjointConceptAxiom]])
  : Try[(OMLTablesResolver, Seq[tables.SpecificDisjointConceptAxiom])]
  = {
    import OMLOps._

    queue
      .foldLeft[Try[(OMLTablesResolver, Map[UUID, Seq[tables.SpecificDisjointConceptAxiom]], Boolean)]](
      Success(Tuple3(resolver, Map.empty, false))
    ) {
      case (Success(Tuple3(ri, mi, fi)), (buuid, x)) =>
        implicit val ex: api.Extent = ri.context.extent
        val gi = ri.context.g
        val bundle = ri.context.bundles(buuid)

        val referencableConcepts
        : Map[tables.UUID, api.Concept]
        = gi.outerNodeTraverser(gi.get(bundle))
          .foldLeft[Map[tables.UUID, api.Concept]](Map.empty)(_ ++ _.concepts.flatMap(e => e.uuid(ex).map(id => id.toString -> e)))

        val referencableConceptTreeDisjunctions
        : Map[tables.UUID, api.ConceptTreeDisjunction]
        = gi.outerNodeTraverser(gi.get(bundle))
          .flatMap {
            case b: api.Bundle => Some(b)
            case _ => None
          }
          .foldLeft[Map[tables.UUID, api.ConceptTreeDisjunction]](Map.empty)(
          _ ++
            _.bundleStatements.flatMap {
                case csd: api.TerminologyBundleStatement with api.ConceptTreeDisjunction =>
                  csd.uuid(ex).map(id => id.toString -> csd)
                case _ =>
                  None
              }
              .toMap)

        val (available, remaining) = x.partition { ax =>
          referencableConceptTreeDisjunctions.contains(ax.disjointTaxonomyParentUUID) &&
            referencableConcepts.contains(ax.disjointLeafUUID)
        }

        val si
        : SortedSet[api.TerminologyBundleStatement]
        = available
          .foldLeft[SortedSet[api.TerminologyBundleStatement]](TreeSet.empty[api.TerminologyBundleStatement]) {
          case (acc, ax) =>
            val x =
              ri.factory.createSpecificDisjointConceptAxiom(
                bundle.uuid(ex),
                referencableConceptTreeDisjunctions(ax.disjointTaxonomyParentUUID),
                referencableConcepts(ax.disjointLeafUUID))

            if (!OMLOps.uuidEquivalent(x.uuid(ex), ax.uuid))
              throw new java.lang.IllegalArgumentException(s"OMLTablesResolver.TerminologyExtends UUID mismatch: read: $ax, created: $x")
            acc + x
        }

        TerminologyContext
          .replaceNode(ri, gi, bundle, bundle.withBundleStatements(si))
          .map { gj =>
            Tuple3(
              ri.copy(context = TerminologyContext(ex, gj)),
              mi + (buuid -> remaining),
              fi || si.nonEmpty)
          }
      case (Failure(t), _) =>
        Failure(t)
    } match {
      case Success(Tuple3(r, m, f)) =>
        if (f)
          resolveSpecificDisjointConceptAxioms(r, m)
        else
          Success(Tuple2(r, m.flatMap(_._2).to[Seq]))
      case Failure(t) =>
        Failure(t)
    }
  }

  type ResolvedAnnotationMap = Map[tables.UUID, Map[api.AnnotationProperty, SortedSet[api.AnnotationEntry]]]
  type AnnotationMapTables = Map[tables.UUID, Map[tables.AnnotationProperty, Seq[tables.AnnotationEntry]]]

  def mergeMapOfSeq[K, V]
  (m1: Map[K, Seq[V]],
   m2: Map[K, Seq[V]])
  : Map[K, Seq[V]]
  = (m1.keySet ++ m2.keySet)
    .map { k =>
      val v1 = m1.getOrElse(k, Seq.empty)
      val v2 = m2.getOrElse(k, Seq.empty)
      k -> (v1 ++ v2)
    }
    .toMap

  def mergeMapOfMapOfSeq[K1, K2, V]
  (mms1: Map[K1, Map[K2, Seq[V]]],
   mms2: Map[K1, Map[K2, Seq[V]]])
  : Map[K1, Map[K2, Seq[V]]]
  = (mms1.keySet ++ mms2.keySet)
    .map { k =>
      val kv1 = mms1.getOrElse(k, Map.empty)
      val kv2 = mms2.getOrElse(k, Map.empty)
      val k2v = mergeMapOfSeq(kv1, kv2)
      k -> k2v
    }
    .toMap

  def mergeMapOfSortedSet[K, V : Ordering]
  (m1: Map[K, SortedSet[V]],
   m2: Map[K, SortedSet[V]])
  : Map[K, SortedSet[V]]
  = (m1.keySet ++ m2.keySet)
    .map { k =>
      val v1 = m1.getOrElse(k, TreeSet.empty[V])
      val v2 = m2.getOrElse(k, TreeSet.empty[V])
      k -> (v1 ++ v2)
    }
    .toMap

  def mergeMapOfMapOfSortedSet[K1, K2, V : Ordering]
  (mms1: Map[K1, Map[K2, SortedSet[V]]],
   mms2: Map[K1, Map[K2, SortedSet[V]]])
  (implicit ex: api.Extent)
  : Map[K1, Map[K2, SortedSet[V]]]
  = (mms1.keySet ++ mms2.keySet)
    .map { k =>
      val kv1 = mms1.getOrElse(k, Map.empty)
      val kv2 = mms2.getOrElse(k, Map.empty)
      val k2v = mergeMapOfSortedSet(kv1, kv2)
      k -> k2v
    }
    .toMap

  def annotationMapC
  (q_u1: (ResolvedAnnotationMap, AnnotationMapTables),
   q_u2: (ResolvedAnnotationMap, AnnotationMapTables))
  (implicit ex: api.Extent)
  : (ResolvedAnnotationMap, AnnotationMapTables)
  = {
    val (q1, u1) = q_u1
    val (q2, u2) = q_u2

    val q = mergeMapOfMapOfSortedSet(q1, q2)
    val u = mergeMapOfMapOfSeq(u1, u2)

    q -> u
  }

  def annotationMapS
  (r: OMLTablesResolver,
   subjects_by_terminology: Map[tables.UUID, (api.TerminologyBox, Map[tables.UUID, api.Element])],
   aps: SortedSet[api.AnnotationProperty])
  (q_u: (ResolvedAnnotationMap, AnnotationMapTables),
   ap_as: (tables.AnnotationProperty, Seq[tables.AnnotationEntry]))
  : (ResolvedAnnotationMap, AnnotationMapTables)
  = {
    implicit val ex: api.Extent = r.context.extent
    val apByUUID: Map[tables.UUID, api.AnnotationProperty] = aps.map { ap => ap.uuid().toString -> ap }.toMap
    val (q1: ResolvedAnnotationMap, u1: AnnotationMapTables) = q_u
    val (ap: tables.AnnotationProperty, as: Seq[tables.AnnotationEntry]) = ap_as

    // Q: is this a known annotation property?
    apByUUID
      .get(ap.uuid)
      .fold[(ResolvedAnnotationMap, AnnotationMapTables)] {

      // A: No, add the annotations to the (unresolved) tables.

      val u2 = as.foldLeft[AnnotationMapTables](u1) { case (ui, a) =>
        val t_pre = ui.getOrElse(a.moduleUUID, Map.empty)
        val t_upd = t_pre.updated(ap, t_pre.getOrElse(ap, Seq.empty) :+ a)
        ui.updated(a.moduleUUID, t_upd)
      }

      q1 -> u2

    } { rap: api.AnnotationProperty =>

      // A: Yes, this is a known annotation property
      // However, the annotations may be asserted in unknown terminologies about unknown subjects.

      // First, partition annotations in terms of assertions attributable to a known terminology
      val (t_resolvable: Seq[tables.AnnotationEntry], t_unresolved: Seq[tables.AnnotationEntry]) =
        as.partition(a => subjects_by_terminology.contains(a.moduleUUID))

      // Second, partition attributable annotations in terms of assertions about known subjects
      val (s_resolvable: Seq[tables.AnnotationEntry], s_unresolved: Seq[tables.AnnotationEntry]) =
        t_resolvable.partition { a =>
          subjects_by_terminology
            .get(a.moduleUUID)
            .exists(_._2.contains(a.subjectUUID))
        }

      val q2: ResolvedAnnotationMap = s_resolvable.foldLeft[ResolvedAnnotationMap](q1) { case (qi, a) =>
        val annotations_by_prop
        : Map[api.AnnotationProperty, SortedSet[api.AnnotationEntry]]
        = qi.getOrElse(a.moduleUUID, Map.empty)

        subjects_by_terminology
          .get(a.moduleUUID)
          .fold[ResolvedAnnotationMap](qi) { case (tbox, subjects) =>
          subjects
            .get(a.subjectUUID)
            .fold[ResolvedAnnotationMap](qi) { subject =>
            val with_a
            : Map[api.AnnotationProperty, SortedSet[api.AnnotationEntry]]
            = annotations_by_prop.updated(
                rap,
                annotations_by_prop.getOrElse(rap, TreeSet.empty[api.AnnotationEntry]) +
                  r.factory.createAnnotationEntry(tbox, subject, a.value))
            qi.updated(a.moduleUUID, with_a)
          }
        }
      }

      val u2: AnnotationMapTables = u1.map { case (tUUID, annotations_by_prop) =>
          val as = annotations_by_prop.getOrElse(ap, Seq.empty) ++ t_unresolved ++ s_unresolved
          tUUID -> annotations_by_prop.updated(ap, as)
      }

      q2 -> u2
    }
  }

  def mapAnnotationPairs
  (r: OMLTablesResolver)
  : Try[OMLTablesResolver]
  = {
    implicit val ex: api.Extent = r.context.extent
    val ns = r.context.tboxes
    val t2everything = ns.map { case (uuid, tbox) =>
        uuid.toString -> (tbox -> tbox.everything().flatMap { e => e.uuid(r.context.extent).map(id => id.toString -> e) }.toMap)
    }

    val (resolved, remaining)
    = r
      .queue
      .annotations.par
      .aggregate[(ResolvedAnnotationMap, AnnotationMapTables)](Map.empty, Map.empty)(
      seqop = annotationMapS(r, t2everything, r.context.extent.annotationProperties),
      combop = annotationMapC)

    val unresolved
    : Map[tables.AnnotationProperty, Seq[tables.AnnotationEntry]]
    = remaining.foldLeft[Map[tables.AnnotationProperty, Seq[tables.AnnotationEntry]]](Map.empty) {
      case (acc, (_, annotations_by_property)) =>
        mergeMapOfSeq(acc, annotations_by_property)
    }
    val r1 = r.copy(queue = r.queue.copy(annotations = unresolved))
    val r2 = resolved.foldLeft[Try[OMLTablesResolver]](Success(r1)) { case (ri, (tUUID, annotations_by_property)) =>
      ri.flatMap { rj =>
        val tbox = rj.context.tboxes(UUID.fromString(tUUID))
        val additions
        : SortedSet[api.AnnotationPropertyTable]
        = annotations_by_property
          .foldLeft[SortedSet[api.AnnotationPropertyTable]](TreeSet.empty[api.AnnotationPropertyTable]) {
          case (acc, (ap, aes)) =>
            acc + rj.factory.createAnnotationPropertyTable(ap, aes)
        }

        val rk = TerminologyContext
          .replaceNode(rj, rj.context.g, tbox, tbox.withAnnotations(additions))
          .map { gi =>
            rj.copy(context = TerminologyContext(rj.context.extent, gi))
          }
        rk
      }
    }
    r2
  }

}
