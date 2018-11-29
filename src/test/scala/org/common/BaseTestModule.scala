package org.common

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by dhiraj
  */

case class SparkObj(sc: SparkContext, sqlContext: SQLContext)

trait BaseTestModule {

  def setup(): SparkObj = {
    val conf = new SparkConf()
      .setMaster("local[1]")
      .setAppName("Basic_SQL_FLOW")
      .set("spark.driver.memory", "1g")
      .set("spark.ui.enabled", "false")
    val sc = new SparkContext(conf)

    //    val hiveContext = new org.apache.spark.sql.hive.HiveContext(sc)
    val sqlContext = new SQLContext(sc)
    SparkObj(sc, sqlContext)

  }

  def close(spark: SparkObj): Unit = {
    spark.sc.stop()
  }



}
