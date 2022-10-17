// Databricks notebook source
// DBTITLE 1,Comparison of major routes for passenger and freight traffic using clustering algorithms
import org.opensky.libadsb._
import org.opensky.libadsb.msgs._
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.functions._

// COMMAND ----------

val df_cls_filter=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_cls_filter_2017_2.parquet")

// COMMAND ----------

val df_test=df_cls_filter.select($"Icao24",$"altitude_list",$"timeAtServer_list",$"longitude_list",$"latitude_list",$"cls",
                                     explode(
                                       arrays_zip($"longitude_list",$"latitude_list",$"timeAtServer_list",$"altitude_list")
                                       .cast("array<struct<longitude:Double,latitude:Double,timeAtServer:Double,altitude:Double>>")
                                       ).as("Position")
                                     ).select($"Icao24",$"Position.longitude" as "lon",$"Position.latitude" as "lat",$"Position.altitude" as "alt",$"Position.timeAtServer" as "timeAtsServer",$"cls")

// COMMAND ----------

val df_round=df_test.selectExpr("Icao24","round(lon,1) as lon","round(lat,1) as lat","round(alt,1) as alt","timeAtsServer as timeAtServer","cls")

// COMMAND ----------

val df_drop=df_round.dropDuplicates("Icao24","cls","lon","lat","alt")

// COMMAND ----------

val df_collected=df_drop.groupBy("Icao24","cls").agg(collect_list($"lon") as "lon_list",collect_list($"lat") as "lat_list",collect_list($"alt") as "alt_list")

// COMMAND ----------

// print(df_collected.count())

// COMMAND ----------

val df_airports = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/2017_airports_with_position.csv").select($"Icao24" as "Icao24_t",$"cls" as "cls_t",$"Name_s",$"Name_e")


// COMMAND ----------

val df_with_name=df_collected.join(df_airports,df_collected("Icao24")===df_airports("Icao24_t")&&df_collected("cls")===df_airports("cls_t")).drop("Icao24_t","cls_t")

// COMMAND ----------

val df_t=df_with_name.selectExpr("Icao24 as Icao24_t","cls as cls_t","lon_list as lon_list_t","lat_list as lat_list_t","alt_list as alt_list_t","Name_s as Name_s_t","Name_e as Name_e_t")

// COMMAND ----------

// print(df_t.count())

// COMMAND ----------

val df_to=df_with_name.join(df_t,df_t("Name_s_t")===df_with_name("Name_s") &&df_t("Name_e_t")===df_with_name("Name_e")).drop("Name_s_t","Name_e_t")//"Icao24_t","cls_t")

// COMMAND ----------

// print(df_to.count())

// COMMAND ----------

import scala.collection.mutable.ListBuffer
object D extends Serializable {
  def editDistance(flight1:Array[String],flight2:Array[String]): Int ={
    val length1=flight1.length;
    val length2=flight2.length;
    val dp=Array.ofDim[Int](length1+1,length2+1)
    for(i<-0 to length1){
      dp(i)(0)=i
    }
    for(j<-0 to length2){
      dp(0)(j)=j
    }

    for(i<- 1 to length1;j <- 1 to length2) {
      val left = dp(i - 1)(j) + 1
      val right = dp(i)(j - 1) + 1
      var diagonal = dp(i - 1)(j - 1)
      if(flight1(i-1)!=flight2(j-1)){
        diagonal+=1
      }
      dp(i)(j)= left min right min diagonal
    }
    //打印状态矩阵测试用的
//    for(i<- 0 until length1+1){
//      for(j<-0 until length2+1){
//        print(dp(i)(j))
//      }
//      println()
//    }
    dp(length1)(length2)
  }
  val EditDistance=udf((flight1:Array[String],flight2:Array[String])=>editDistance(flight1,flight2))
  
  def agg_to_str(lon:Array[Double],lat:Array[Double])={
    var res=new ListBuffer[String]
    val length=lon.length
    for(i<- 0 until length){
      res.append(lon(i).toString+lat(i).toString)
    }
    res
  }
  val AggToStr=udf((lon:Array[Double],lat:Array[Double])=>agg_to_str(lon,lat))
  
}

// COMMAND ----------

val df_str=df_to.withColumn("flight1",D.AggToStr($"lon_list",$"lat_list")).withColumn("flight2",D.AggToStr($"lon_list_t",$"lat_list_t")).withColumn("editD",D.EditDistance($"flight1",$"flight2"))

// COMMAND ----------

// display(df_str)

// COMMAND ----------

// display(df_str.select("editD"))

// COMMAND ----------

// val df_airlines = spark.read.format("csv").option("header","true")
//                 .load("dbfs:/FileStore/group16/airlines_2.csv").drop("_c3").drop("name").select($"type",$"callsign".alias("Callsign_tempt"))
val df_airlines = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/2017_airports_with_position.csv").dropDuplicates("callsign").select($"type",$"callsign".alias("Callsign_tempt"))
val df_map=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_icao_callsign_2017.parquet").select($"Icao".alias("Icao_t"),$"Callsign".alias("Callsign_t"))

// COMMAND ----------

val df_get_callsign=df_str.join(df_map,df_map("Icao_t")===df_str("Icao24_t")).drop("Icao_t")
val df_get_type=df_get_callsign.join(df_airlines,df_airlines("Callsign_tempt")===df_get_callsign("Callsign_t")).drop("Callsign_tempt")

// COMMAND ----------

import org.apache.spark.sql.expressions.Window
val e_window=Window.partitionBy("Name_s","Name_e")
val df_exist=df_get_type.withColumn("exist_cargo",sum("type").over(e_window)).filter($"exist_cargo"=!=0)// .drop("exist_cargo")
// display(df_exist)

// COMMAND ----------



// COMMAND ----------

import org.apache.spark.sql.expressions.Window
val df_ordered=df_exist.groupBy("Name_s","Name_e","cls","Icao24","Callsign_t","type").agg(sum($"editD") as "Addition")

// COMMAND ----------

val w=Window.partitionBy("Name_s","Name_e")
// val df_min=df_ordered.withColumn("minD",min("Addition").over(w)).filter($"minD"===$"Addition").drop("Addition").selectExpr("Name_s as Name_s_t","Name_e as Name_e_t","cls as cls_t","Icao24 as Icao24_t","minD")
val df_min=df_ordered.withColumn("minD",min("Addition").over(w)).filter($"Addition"===$"minD").drop("Addition").selectExpr("Name_s as Name_s_t","Name_e as Name_e_t","cls as cls_t","Icao24 as Icao24_t","minD","Callsign_t","type")

// COMMAND ----------

val df_groupBy=df_min.groupBy("Name_s_t","Name_e_t").agg(sum("type")) //看出客货的 main route 区别不大
// display(df_groupBy)

// COMMAND ----------

df_groupBy.write.option("header", "true").csv("dbfs:/mnt/group16/main_route_2017_111
.csv")

// COMMAND ----------

// dbutils.fs.rm("dbfs:/mnt/group16/main_route_2017.csv",true)

// COMMAND ----------

display(df_groupBy)

// COMMAND ----------



// COMMAND ----------



// COMMAND ----------

val df_res=df_str.join(df_min,df_min("cls_t")===df_str("cls") &&df_min("Icao24_t")===df_str("Icao24") &&df_min("Name_s_t")===df_str("Name_s") &&df_min("Name_e_t")===df_str("Name_e")).drop("Name_s_t","Name_e_t","cls_t","Icao24_t","flight1","flight2","lat_list_t","alt_list_t","lon_list_t","editD")

// COMMAND ----------

import org.apache.spark.sql.functions._
val df_test=df_res.select($"Icao24",$"alt_list",$"lon_list",$"lat_list",$"cls",$"Name_s",$"Name_e",$"minD",
                                     explode(
                                       arrays_zip($"lon_list",$"lat_list",$"alt_list")
                                       .cast("array<struct<longitude:Double,latitude:Double,altitude:Double>>")
                                       ).as("Position")
                                     ).select($"Icao24",$"Position.longitude" as "lon",$"Position.latitude" as "lat",$"Position.altitude" as "alt",$"cls",$"Name_s",$"Name_e",$"minD")

// COMMAND ----------

// display(df_test)

// COMMAND ----------

df_test.write.option("header","true").csv("dbfs:/mnt/group16/df_cluster_route_2016.csv")

// COMMAND ----------

