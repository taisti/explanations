# Constraint violation explainer

This is a proof of concept for a tool to explain why a particular food product (e.g., an ingredient in a recipe, or a 
substitute for it) is not suitable in a particular diet

## Requirements

* Java >=17
* Maven

## Building

In the root directory of the repository execute the following command:
```shell
mvn clean package
```

It will download the necessary dependencies and build the final JAR

## Running

To start the program run the following in the root directory of the repository (after it has been successfully built)

```shell
java -jar target/T3_3-1.0-SNAPSHOT.jar
```

The program is equipped with only text user interface. Follow the instructions of the program.