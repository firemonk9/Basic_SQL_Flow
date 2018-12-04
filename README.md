# Project Basic_SQL_Flow

Basic_SQL_Flow is a work flow based tool for sql statements. The input is a JSON or CSV file composed of jobs represented as a tree. The following five job types are supported.
* Filter
* Transformation
* Join
* Column cast
* Supports following Input/Output source 

The Input/Output can be any of the following five types of data sources.
1. CSV
2. Json 
3. Hive flow
4. JDBC 
5. Parquet


## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing. See deployment for notes on how to deploy the project on a live system.

Clone the Repo from URL: 

```git clone https://github.com/firemonk9/Basic_SQL_Flow```

### Prerequisites for development


* Java8
* Java editor (Intellij preferred)

```
Give examples
```

### Building

The following command will create uber jar.
```
sbt assembly
```

## Running the tests

The following command executes tests. 
```
sbt test
```

## Deployment

Copy the data_wf.jar jar to the server. Below is a an example to run the sample using spark-submit command.

replace the <CODE_PATH> with location where the project is downloaded/cloned in below command and also <CODE_PATH>/Basic_SQL_Flow/src/test/resources/flow1.json

```
spark-submit --class org.wf.SqlWorkFlowMain <CODE_PATH>/Basic_SQL_Flow/target/scala-2.11/data_wf.jar  INPUT_FILE=<CODE_PATH>/Basic_SQL_Flow/src/test/resources/flow1.json DEBUG=false local:true
```

## Built With

* [Sbt](https://www.scala-sbt.org) - Dependency Management

## Authors

* **Dhiraj Peechara** - *Initial work* 


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Hat tip to anyone whose code was used
* Inspiration
* etc
