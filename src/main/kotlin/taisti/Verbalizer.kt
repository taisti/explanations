package taisti

import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.model.parameters.Imports
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence

class Verbalizer(val ontology: OWLOntology) {

    fun filterAndOrder(axioms: Set<OWLAxiom>, root: OWLEntity, visited: Set<OWLEntity>): List<OWLAxiom> {
        val relevant = ArrayList<OWLAxiom>()
        for (axiom in axioms) {
            if (axiom.containsEntityInSignature(root))
                relevant.add(axiom)
        }
        val newVisited = visited + setOf(root)
        val remaining = axioms - relevant.toSet()
        (relevant.asSequence()
            .flatMap { it.classesInSignature().asSequence() + it.individualsInSignature().asSequence() }
            .toSet() - newVisited)
            .flatMapTo(relevant) { filterAndOrder(remaining, it, newVisited) }
        return relevant
    }

    fun OWLObject?.hasIRI(iri: String) = (this as? HasIRI)?.iri == IRI.create(iri)

    fun verbalize(axiom: OWLObjectPropertyAssertionAxiom): String? {
        val subject = verbalize(axiom.subject) ?: return null
        val p = verbalize(axiom.property) ?: return null
        val obj = verbalize(axiom.`object`) ?: return null
        return "'$subject' $p '$obj'"
    }

    fun verbalize(property: OWLPropertyExpression): String? =
        when {
            property.hasIRI("http://www.semanticweb.org/subFoodV2#has_member") -> "contains"
            property.hasIRI("http://www.semanticweb.org/subFoodV2#is_about") -> "refers to"
            property.hasIRI("http://www.semanticweb.org/subFoodV2#component_of") -> null
            property is HasIRI -> verbalize(property.iri)
            else -> TODO()
        }

    fun verbalize(axiom: OWLObjectSomeValuesFrom): String? {
        val p = verbalize(axiom.property)
        val f = verbalize(axiom.filler) ?: return null
        return "$p '$f'"
    }

    fun verbalize(axiom: OWLClassAssertionAxiom): String? {
        val ind = verbalize(axiom.individual) ?: return null
        val cls = verbalize(axiom.classExpression) ?: return null
        return "'$ind' $cls"
    }

    fun verbalize(axiom: OWLSubClassOfAxiom): String? {
        if (axiom.subClass is OWLClass && axiom.superClass is OWLObjectSomeValuesFrom) {
            val left = verbalize(axiom.subClass)
            val right = verbalize(axiom.superClass as OWLObjectSomeValuesFrom)
            return "'$left' $right"
        }
        if (axiom.subClass is OWLClass && axiom.superClass is OWLClass) {
            val left = verbalize(axiom.subClass)
            val right = verbalize(axiom.superClass)
            return "'$left' is '$right'"
        }
        if (axiom.subClass is OWLObjectSomeValuesFrom && axiom.superClass is OWLObjectSomeValuesFrom) {
            val left = verbalize(axiom.subClass)
            val right = verbalize(axiom.superClass)
            return "If something $left, then it $right"
        }
        return axiom.toString()
    }

    fun verbalize(iri: IRI): String? = ontology.annotationAssertionAxioms(iri, Imports.INCLUDED).asSequence()
        .firstOrNull { it.property.hasIRI("http://www.w3.org/2000/01/rdf-schema#label") }?.value?.asLiteral()
        ?.getOrNull()?.literal
        ?: iri.toString()

    fun verbalize(obj: OWLObject): String? =
        when (obj) {
            is OWLObjectPropertyAssertionAxiom -> verbalize(obj)
            is OWLClassAssertionAxiom -> verbalize(obj)
            is OWLSubClassOfAxiom -> verbalize(obj)
            is OWLObjectSomeValuesFrom -> verbalize(obj)
            is OWLEntity -> verbalize(obj.iri)
            else -> TODO(obj::class.toString())
        }

    fun run(axioms: Set<OWLAxiom>, root: OWLEntity): List<String> =
        filterAndOrder(axioms, root, emptySet()).mapNotNull {
            verbalize(it)?.ifBlank { null }
        }
}