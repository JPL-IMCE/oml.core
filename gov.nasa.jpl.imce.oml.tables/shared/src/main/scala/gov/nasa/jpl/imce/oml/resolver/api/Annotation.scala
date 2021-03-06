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

package gov.nasa.jpl.imce.oml.resolver.api

/*
 * An OML Annotation maps to an [OWL2 Annotation] and is similarly
 * a non-logical statement in an OML Module
 * associating some information as the value of an
 * OML AnnotationProperty for describing a subject (an OML TerminologyThing).
 */
trait Annotation
{

  val property: AnnotationProperty
  val subject: Element
  val value: scala.Predef.String
}
