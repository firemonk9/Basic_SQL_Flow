package org.wf

//org.diff.DataDiffTool
import java.io.ByteArrayOutputStream
import javax.xml.bind.DatatypeConverter
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.common.{DiffToolJsonParser, JobsExecutor}
import org.common.model._
import org.wf.util.FileUtil

/**
  * Created by dhiraj
  */
object SqlWorkFlowMain extends SparkInit with DiffToolJsonParser {


  var submittedTime: Option[Long] = None
  var LICENSED: Boolean = false
  var jobId: String = null
  var execId: String = null
  var projectId: String = null
  var base64enCoded: Boolean = false
  var exceptionOccured: Boolean = false

  def setSparkContext(lsparkContext: SparkSession): Unit = {
    sparkContextLivy = lsparkContext
  }

  var sparkContextLivy: SparkSession = null

  def main(args: Array[String]) {

    System.out.println("args = " + args.toList)
    val argsMap = parseArgs(args)

    val inputDiffPropsFile = argsMap.getOrElse("INPUT_FILE", "")

    val debug = argsMap.get("DEBUG")
    val local = if (argsMap.getOrElse("local", "false") == "true") true else false
    val spark = if (sparkContextLivy != null) {
      sparkContextLivy
    } else if (local == false) {
      SparkSession.builder().appName("SQLWorkFlow").enableHiveSupport().getOrCreate()
    }
    else {
      SparkSession.builder().appName("SQLWorkFlow").master("local").getOrCreate()
    }

    val sQLContext = spark.sqlContext

    import java.util.Properties

    import org.apache.log4j.PropertyConfigurator

    val props = new Properties()
    props.load(getClass.getClassLoader.getResourceAsStream("log4j.properties"))
    PropertyConfigurator.configure(props)
    val hdfsOutput: Boolean = if (argsMap.getOrElse("HDFS_OUTPUT", "true") == "true") true else false

    try {
      processDiff1(inputDiffPropsFile, sQLContext, machineConsumable1 = false, hdfsOutput)
    } catch {
      case e: Exception => e.printStackTrace(); throw e;
    } finally {
      spark.stop()
    }
  }


  def processDiff1(inputFile: String, sqlContext: SQLContext, machineConsumable1: Boolean = true, hdfsOutput: Boolean = true): Unit = {
    val jsonContent = if (base64enCoded) {
      Some(new String(DatatypeConverter.parseBase64Binary(inputFile), "utf-8"))
    } else if (hdfsOutput) org.wf.util.FileUtil.getFileContent(inputFile, sqlContext.sparkContext.hadoopConfiguration) else Some(scala.io.Source.fromFile(inputFile).getLines().mkString)
    val filesCompare: InputFlow = if (jsonContent.isDefined) readDataTaskChainsJson(jsonContent.get) else throw new IllegalArgumentException("unable to read input JSON " + inputFile)
    org.common.JobsExecutor.init(sqlContext)
    JobsExecutor.processChains(filesCompare, sqlContext)
//    exceptionOccured = res.exception
  }

  def writeToFile(jsonStr: String, filePath: Option[String], hdfsOutput: Boolean, sqlContext: SQLContext): Unit = {
    if (filePath.isDefined) {
      if (hdfsOutput) FileUtil.writeToFile(filePath, jsonStr, sqlContext.sparkContext.hadoopConfiguration) else FileUtil.writeToTextFile(filePath.get, jsonStr)
    }
  }

  def compress(input: String): Array[Byte] = {

    import java.util.zip.{ZipEntry, ZipOutputStream}

    val path = System.getProperty("java.io.tmpdir") + "/" + "flow_result.json"
    val baos = new ByteArrayOutputStream()
    val zos = new ZipOutputStream(baos)
    /* File is not on the disk, test.txt indicates
        only the file name to be put into the zip */
    val entry = new ZipEntry("flow_result.json")
    zos.putNextEntry(entry)
    zos.write(input.getBytes)
    zos.closeEntry()
    baos.toByteArray

  }


  def parseArgsJava(args: Array[String]): java.util.Map[String, String] = {
    import scala.collection.JavaConverters._
    parseArgs(args).asJava
  }

  def parseArgs(args: Array[String]): scala.collection.Map[String, String] = {
    val v: scala.collection.Map[String, String] = args.filter(a => a.contains("=")).map(a => {
      val ar = a.splitAt(a.indexOf("="))
      val res = ar._1 -> ar._2.substring(1, ar._2.length)

      res
    }).toMap
    v
  }


}