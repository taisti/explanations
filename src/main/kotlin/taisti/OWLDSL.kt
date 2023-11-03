package taisti

import org.semanticweb.owlapi.model.*

class OWLDSL(val prefix: String? = null, val factory: OWLDataFactory) {
    fun iri(iri: String) =
        if (iri.startsWith("http")) IRI.create(iri) else prefix?.let { IRI.create(prefix, iri) } ?: IRI.create(iri)

    fun ind(iri: String) = factory.getOWLNamedIndividual(iri(iri))
    fun op(iri: String) = factory.getOWLObjectProperty(iri(iri))
    fun cls(iri: String) = factory.getOWLClass(iri(iri))

    infix fun OWLClassExpression.and(right: OWLClassExpression) = factory.getOWLObjectIntersectionOf(this, right)
    infix fun OWLObjectProperty.some(filler: OWLClassExpression) = factory.getOWLObjectSomeValuesFrom(this, filler)
    infix fun OWLIndividual.a(cls: OWLClassExpression) = factory.getOWLClassAssertionAxiom(cls, this)
}

fun <R> OWLDataFactory.manchester(prefix: String? = null, block: OWLDSL.() -> R): R = OWLDSL(prefix, this).block()
