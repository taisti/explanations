package taisti

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator
import com.clarkparsia.owlapi.explanation.util.SilentExplanationProgressMonitor
import org.semanticweb.HermiT.Configuration
import org.semanticweb.HermiT.EntailmentChecker
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLNamedIndividual
import java.util.*
import kotlin.system.exitProcess

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val manager = OWLManager.createOWLOntologyManager()
        val factory = manager.owlDataFactory
        val ontology =
            manager.loadOntologyFromOntologyDocument(Main::class.java.classLoader.getResourceAsStream("kg.owl")!!)
        val reasoner = Reasoner(Configuration(), ontology)
        val checker = EntailmentChecker(reasoner, manager.owlDataFactory)
        val verbalizer = Verbalizer(ontology)
        val stdin = Scanner(System.`in`)

        val variants = factory.manchester("http://www.semanticweb.org/subFoodV2#") {
            listOf(
                "Gluten allergy" to (op("has_allergic_trigger") some cls("gluten_allergy")),
                "Sesame allergy" to (op("has_allergic_trigger") some cls("sesame_allergy")),
                "Soy allergy" to (op("has_allergic_trigger") some cls("soy_allergy")),
                "Vegan diet" to (op("unacceptable_In") some cls("http://purl.obolibrary.org/obo/ONS_1000021")),
                "Gluten-free diet" to (op("unacceptable_In") some cls("http://purl.obolibrary.org/obo/ONS_1000043")),
            )
        }

        val recipes =
            reasoner.getInstances(factory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/FOODON_00004081")))
                .entities().toList()

        while (true) {
            for ((i, r) in recipes.withIndex()) {
                println("${i + 1}. ${verbalizer.verbalize(r.iri)}")
            }
            println("0. Exit")
            val recipe = with(stdin.nextInt() - 1) {
                if (this in recipes.indices) recipes[this]
                else exitProcess(0)
            }

            for ((i, v) in variants.withIndex()) {
                println("${i + 1}. ${v.first}")
            }
            println("0. Exit")
            val variant = with(stdin.nextInt() - 1) {
                if (this in variants.indices) variants[this].second
                else exitProcess(0)
            }

            val axiom = factory.getOWLClassAssertionAxiom(variant, recipe)

            val result = checker.entails(axiom)

            if (!result) {
                println("The recipe conforms to the requirements")
                println()
                continue
            }

            val explanationGenerator = DefaultExplanationGenerator(
                manager,
                org.semanticweb.HermiT.ReasonerFactory(),
                ontology,
                reasoner,
                SilentExplanationProgressMonitor()
            )
            val explanation = explanationGenerator.getExplanation(axiom)
            println("The recipe does not conform to the requirements, because:")
            verbalizer.run(explanation, axiom.individual as OWLNamedIndividual)
                .forEachIndexed { index, s -> println("${index + 1}. $s") }
            println()
        }
    }
}