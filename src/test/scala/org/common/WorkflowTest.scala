package org.common

import java.nio.file.Paths

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions.expr
import org.common.{DiffToolJsonParser, JobsExecutor}
import org.common.model._
import org.wf.WorkFlowUtil
import org.wf.WorkFlowUtil.getDelimiter
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class WorkflowTest extends FunSuite with BaseTestModule with BeforeAndAfterAll with DiffToolJsonParser {


  var vv: SparkObj = null
  var sqlContext: SQLContext = null

  override def beforeAll(): Unit = {
    vv = setup()
    sqlContext = vv.sqlContext
  }


  override def afterAll() {
    vv.sc.stop()
    // println("After!") // shut down the web server
  }

  test("basic flow 1") {
    val sqlContext = vv.sqlContext //new org.apache.spark.sql.SQLContext(vv.sc)
    import sqlContext.implicits._

    val basePath = Paths.get(this.getClass().getResource("/sample_data/").toURI()).toAbsolutePath.toString
    val job1 = Job("source1", sourceData = Some(FileSource(basePath + "/golden_src.csv", "CSV", header = Some(true))), jobOutputTableName = Some("source1"))
    val job2 = Job("filter", dependsOn = Some(List("source1")), filterData = Some(FilterData(Some("select * from source1 where name not like '%ZZ%'"))), jobOutputTableName = Some("source1_filter"))
    val job3 = Job("output", dependsOn = Some(List("filter")), output = Some(FileSource(basePath + "/golden_src_temp1.csv", "CSV", header = Some(true), tableNameMap = Some(Map("source1_filter" -> (basePath + "/golden_src_temp1.csv"))))))

    val stringToJob = Map("source1" -> job1, "filter" -> job2, "output" -> job3)
    JobsExecutor.processChains(InputFlow("output", stringToJob, flowName = "my_temp"), sqlContext)
    val df1 = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(basePath + "/golden_src.csv") //.save(outputDir)
    val df2 = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(basePath + "/golden_src_temp1.csv") //.save(outputDir)

    val df3 = df1.except(df2)
    assert(df3.count() == 0)

  }

  test("basic flow with filter some data ") {
    val sqlContext = vv.sqlContext //new org.apache.spark.sql.SQLContext(vv.sc)
    import sqlContext.implicits._

    val basePath = Paths.get(this.getClass().getResource("/sample_data/").toURI()).toAbsolutePath.toString
    val job1 = Job("source1", sourceData = Some(FileSource(basePath + "/golden_src.csv", "CSV", header = Some(true))), jobOutputTableName = Some("source1"))
    val job2 = Job("filter", dependsOn = Some(List("source1")), filterData = Some(FilterData(Some("select * from source1 where name not like '%Linus Avila%'"))), jobOutputTableName = Some("source1_filter"))
    val job3 = Job("output", dependsOn = Some(List("filter")), output = Some(FileSource(basePath + "/golden_src_temp1.csv", "CSV", header = Some(true), tableNameMap = Some(Map("source1_filter" -> (basePath + "/golden_src_temp1.csv"))))))

    val stringToJob = Map("source1" -> job1, "filter" -> job2, "output" -> job3)
    JobsExecutor.processChains(InputFlow("output", stringToJob, flowName = "my_temp"), sqlContext)
    val df1 = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(basePath + "/golden_src.csv") //.save(outputDir)
    val df2 = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(basePath + "/golden_src_temp1.csv") //.save(outputDir)

    val df3 = df1.except(df2)
    assert(df3.count() == 3)

  }


  test("basic flow with filter and transforamtion ") {
    val sqlContext = vv.sqlContext //new org.apache.spark.sql.SQLContext(vv.sc)
    import sqlContext.implicits._

    val basePath = Paths.get(this.getClass().getResource("/sample_data/").toURI()).toAbsolutePath.toString
    val job1 = Job("source1", sourceData = Some(FileSource(basePath + "/golden_src.csv", "CSV", header = Some(true))), jobOutputTableName = Some("source1"))
    val job2 = Job("filter", dependsOn = Some(List("source1")), filterData = Some(FilterData(Some("select * from source1 where name not like '%Linus Avila%'"))), jobOutputTableName = Some("source1_filter"))

    val job3 = Job("transformation", dependsOn = Some(List("filter")), dataTransformRule = Some(List(DataTransformRule(column = Some(List("name")), columnExpressionSqlStatement = Some("substring(name,0,8) as name")))), jobOutputTableName = Some("source1_transform"))
    val tempOutputFile = "golden_src_temp1.csv"
    val job4 = Job("output", dependsOn = Some(List("transformation")), output = Some(FileSource(basePath + "/" + tempOutputFile, "PARQUET", header = Some(true), tableNameMap = Some(Map("source1_transform" -> (basePath + "/" + tempOutputFile + ".csv"))))))

    val stringToJob = Map("source1" -> job1, "filter" -> job2, "transformation" -> job3, "output" -> job4)
    val flow = InputFlow("output", stringToJob, flowName = "my_temp")

    val jsonStr = writeDataTaskChainsJson(flow)
    println(jsonStr)

    JobsExecutor.processChains(flow, sqlContext)
    val df1 = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(basePath + "/golden_src.csv") //.save(outputDir)
    val wf_produced_output = sqlContext.read.parquet(basePath + "/" + tempOutputFile + ".csv") //.save(outputDir)

    df1.createOrReplaceTempView("t2")
    val test_output = sqlContext.sql("select substring(name,0,8) as name,company,phone,email,date,street from t2 where name not like '%Linus Avila%'")

    val columns = test_output.columns
    val df3 = wf_produced_output.select(columns.head, columns.tail: _*).except(test_output.select(columns.head, columns.tail: _*))

    df3.show(true)
    import org.apache.spark.sql.functions.expr
    wf_produced_output.select(columns.head, columns.tail: _*).withColumn("length", expr("length(name)")).show(true)
    test_output.select(columns.head, columns.tail: _*).withColumn("length", expr("length(name)")).show(true)

    println(" basePath :  " + basePath)
    assert(df3.count() == 0)

  }


  test("test filter transform and join") {
    val sqlContext = vv.sqlContext //new org.apache.spark.sql.SQLContext(vv.sc)
    import sqlContext.implicits._

    val basePath = Paths.get(this.getClass().getResource("/sample_data/").toURI()).toAbsolutePath.toString
    val joba = Job("source_a", sourceData = Some(FileSource(basePath + "/joina.csv", "CSV", header = Some(true))), jobOutputTableName = Some("joina"))
    val jobb = Job("source_b", sourceData = Some(FileSource(basePath + "/joinb.csv", "CSV", header = Some(true))), jobOutputTableName = Some("joinb"))
    val filterJobA = Job("filter_a", dependsOn = Some(List("source_a")), filterData = Some(FilterData(Some("select * from joina where A not like '%Linus Avila%'"))), jobOutputTableName = Some("filterJobA"))
    val joinab = Job("join_a_b", dependsOn = Some(List("source_a", "source_b")), joins = Some(JoinJob(Some("select a.A,B,C,D from joina a inner join joinb b on a.A=b.A"))), jobOutputTableName = Some("join_output"))
    val tempOutputFile = "golden_src_temp1.csv"

    val outputResult = Job("output", dependsOn = Some(List("join_a_b")), output = Some(FileSource(basePath + "/" + tempOutputFile, "PARQUET", header = Some(true), tableNameMap = Some(Map("join_output" -> (basePath + "/" + tempOutputFile))))))
    val stringToJob = Map("source_a" -> joba, "source_b" -> jobb, "filter_a" -> filterJobA, "join_a_b" -> joinab, "output" -> outputResult)
    val flow = InputFlow("output", stringToJob, flowName = "my_temp")
    val jsonStr = writeDataTaskChainsJson(flow)
    println(jsonStr)
    JobsExecutor.processChains(flow, sqlContext)

    val test_output = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(basePath + "/joinc.csv")
    val wf_produced_output = sqlContext.read.parquet(basePath + "/" + tempOutputFile)

    val columns = test_output.columns
    val value = test_output.select(columns.head, columns.tail: _*).except(wf_produced_output.select(columns.head, columns.tail: _*))
    val badRecordsCount = value.count()
    value.show()
    wf_produced_output.show(true)
    test_output.show(true)
    assert(badRecordsCount == 0)


  }



}
