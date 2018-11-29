package org.common

import java.util

import org.apache.spark.sql.{DataFrame, SQLContext}
import org.common.model._
import org.wf.WorkFlowUtil


case class DFName(df: Option[DataFrame], name: String)

object JobsExecutor {


  var dfMap: java.util.Map[Job, DFName] = new util.HashMap[Job, DFName]()
//  var jobResultMap: java.util.HashMap[String, JobAndResMap] = new util.HashMap[String, JobAndResMap]()
  var exception: Boolean = false
  //val EXCEPTION: String = "EXCEPTION"


  var sqlContext: SQLContext = null

  def init(lsqlContext: SQLContext): Unit = {
//    jobResultMap = new util.HashMap[String, JobAndResMap]()
    exception = false
    dfMap = new util.HashMap[Job, DFName]()
    sqlContext = lsqlContext: SQLContext
  }

  var ljobs: Map[String, Job] = null

  def processChains(inputFlow: InputFlow, lsqlContext: SQLContext): Unit = {

    sqlContext = lsqlContext

    ljobs = inputFlow.jobMap
//    ljobs.values.foreach(job => {
//      jobResultMap.put(job.id, JobAndResMap(job, Some(JobResult(DiffConstants.CANCELLED, None, None, None, None, None, None, None, None))))
//    })
    postOrderTraversal(ljobs.get(inputFlow.root))

    import scala.collection.JavaConverters._
//    val myScalaMap: Map[String, JobAndResMap] = jobResultMap.asScala.toMap //.mapValues(_.asScala.toSet)
//    FlowResults(myScalaMap, inputFlow.flowName, exception, inputFlow.root)
  }


  def postOrderTraversal(node: Option[Job]): Unit = {
    if (node.isEmpty)
      return

    if (node.get.dependsOn.isDefined) {
      node.get.dependsOn.get.foreach(b => {
        postOrderTraversal(ljobs.get(b))
      })
    }

    val resException = processJob(node.get)
    if (resException == true) throw new Exception(" exception occurred in processing the workflow for the step : " + node.get)

  }


  def extractFileName(datasetPath: String) = {
    if (datasetPath.indexOf("/") > 0) datasetPath.substring(datasetPath.lastIndexOf("/"), datasetPath.lastIndexOf(".") + 1) else datasetPath
  }

  def processJob(l1: Job): Boolean = {

    var result: Option[JobResult] = None
    var joinJob: Option[Job] = None
    val a = l1
    val startTime = Some(System.nanoTime())

    def exeJob(e: Exception) = {
      e.printStackTrace()
//      result = Some(JobResult(DiffConstants.EXCEPTION, None, None, Some(e.getMessage), Some(e.getStackTrace.mkString("\n")), startTime, Some(System.currentTimeMillis())))
//      val t = JobAndResMap(a, result)
//      jobResultMap.put(a.id, t)
      exception = true
    }

    println("processing : " + l1)
    val temp = l1

    var df: Option[DFName] = try {
      val sourceName = if (l1.sourceData.isDefined) None else if (l1.dependsOn.isDefined && l1.dependsOn.get.length == 1) Some(getDFName(l1.dependsOn.get.head)) else None
      if (sourceName.isDefined) Some(DFName(Some(sqlContext.sql("select * from " + sourceName.get)), sourceName.get)) else None
    } catch {
      case e: Exception => exeJob(e); None
    }


    if (a.dataTransformRule.isDefined) {
      try {
        // we want to cache after transformation.
        println("in transform ::")
        val tdf = WorkFlowUtil.transformInputData(df.get.df.get, a.dataTransformRule, Map()).cache()
        val sourceName = if (a.jobOutputTableName.isDefined) a.jobOutputTableName.get else df.get.name
        df = Some(DFName(Some(tdf), sourceName))
//        val tempMap = JobAndResMap(a, Some(JobResult(DiffConstants.COMPLETED, None, None, None, None, startTime, Some(System.currentTimeMillis()))))
//        jobResultMap.put(a.id, tempMap)
        df.get.df.get.show()

      } catch {
        case e: Exception => exeJob(e)
      }
    }
    else if (a.filterData.isDefined) {
      try {
        println("in filter ::")
        df.get.df.get.show()
        val tdf = WorkFlowUtil.filterDF(df.get.df.get, None, a.filterData.get.filterSql, sqlContext)
        val sourceName = if (a.jobOutputTableName.isDefined) a.jobOutputTableName.get else df.get.name
        df = Some(DFName(Some(tdf), sourceName))
//        val tempMap = JobAndResMap(a, Some(JobResult(DiffConstants.COMPLETED, None, None, None, None, startTime, Some(System.currentTimeMillis()))))
//        jobResultMap.put(a.id, tempMap)

      } catch {
        case e: Exception => exeJob(e)

      }
    }
    else if (a.sourceData.isDefined) {
      try {
        println("in source data ::")
        val kValue = if (a.sourceData.get.tableName.isDefined) a.sourceData.get.tableName.get else a.sourceData.get.datasetPath
        val mapTableName: Map[String, String] = if (a.sourceData.get.tableNameMap.isDefined) a.sourceData.get.tableNameMap.get else Map(a.sourceData.get.datasetPath -> kValue)

        mapTableName.keys.foreach(key => {
          val tableName = if (mapTableName.get(key).get != null && mapTableName.get(key).get.length() > 0) mapTableName.get(key).get else key
          val tdf = WorkFlowUtil.createDataFrame(a.sourceData.get, sqlContext).cache()
          df = Some(DFName(Some(tdf), tableName))
          df.get.df.get.show()
        })

//        val someResult = JobAndResMap(a, Some(JobResult(DiffConstants.COMPLETED, None, None, None, None, startTime, Some(System.currentTimeMillis()))))
//        jobResultMap.put(a.id, someResult)

      } catch {
        case e: Exception => exeJob(e)
      }
    }
    else if (a.joins.isDefined) {
      try {
        val (resultDF, joinCount) = WorkFlowUtil.joinDF(a.joins.get, sqlContext)
        df = Some(DFName(Some(resultDF), a.id))
//        val someResult = JobAndResMap(a, Some(JobResult(DiffConstants.COMPLETED, Some(joinCount), None, None, None, startTime, Some(System.currentTimeMillis()))))
//        jobResultMap.put(a.id, someResult)

      } catch {
        case e: Exception => exeJob(e)
      }
    } else if (a.output.isDefined && df.isDefined) {
      try {
        println("in output data ::")
        val kValue = if (a.output.get.tableName.isDefined) a.output.get.tableName.get else a.output.get.datasetPath
        val mapTableName: Map[String, String] = a.output.get.tableNameMap.get //else Map(a.output.get.datasetPath -> kValue)

        mapTableName.keys.foreach(key => {

          val ttdf = sqlContext.sql("select * from " + key)
          val tdf = WorkFlowUtil.writeDataframe(ttdf, a.output.get, mapTableName.get(key).get, sqlContext)
        })

//        val someResult = JobAndResMap(a, Some(JobResult(DiffConstants.COMPLETED, None, None, None, None, startTime, Some(System.currentTimeMillis()))))
//        jobResultMap.put(a.id, someResult)

      } catch {
        case e: Exception => exeJob(e)
      }
    }


    if (df.isDefined && df.get.df.isDefined) {
      try {

        val str = if (l1.jobOutputTableName.isDefined) l1.jobOutputTableName.get else l1.id
        df.get.df.get.createOrReplaceTempView(str)
        println("setting the name : " + str + " for below sample dataframe")
        sqlContext.sql("select * from " + str).show()
      } catch {
        case e: Exception => exeJob(e)
      }
    }
    return exception

  }


  private def getDFName(headDependency: String): String = {

    println(" headDependency " + headDependency + "   and jobResultMap.get(headDependency) : " + ljobs.get(headDependency).get + "  jobResultMap.get(headDependency).job.jobOutputTableName.get : ")
    val tableName = if (ljobs.get(headDependency) != null && ljobs.get(headDependency).get.jobOutputTableName.isDefined) ljobs.get(headDependency).get.jobOutputTableName.get else headDependency
    println(" headdependency : " + headDependency + "  tableName " + tableName)
    return tableName

  }
}
