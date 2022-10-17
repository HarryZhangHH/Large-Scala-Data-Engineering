// Databricks notebook source
import org.opensky.libadsb._
import org.opensky.libadsb.msgs._
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.functions._

// COMMAND ----------

val df_cls_filter=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_cls_filter_2017_2.parquet")

// COMMAND ----------

import scala.collection.mutable.ListBuffer
object Tracker extends Serializable{
  def rad(number:Double): Double ={
    number*Math.PI/180
  }
  def distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = { //返回的是公里为单位的距离数
    val EARTH_RADIUS = 6378.137
    val radLat1 = rad(lat1)
    val radLat2 = rad(lat2)
    val a = rad(lat1) - rad(lat2)
    val b = rad(lng1) - rad(lng2)
    val distance = EARTH_RADIUS * 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin((b) / 2), 2)))
    //    printLog.debug("lat1: " + lat1 + " lng1: " + lng1 + " lat2: " + lat2 + " lng2: " + lng2)
//     println("distance:" + distance)
    distance
  }
  
  def getPointCluster(timeAtServer:Array[Double],alt:Array[Double],lng:Array[Double],lat:Array[Double])={
    val length=timeAtServer.length
    var cluster_number=0
    var res=ListBuffer(cluster_number)
    for(i<-5 until length-5){
      if(((timeAtServer(i)-timeAtServer(i-1)>1200000 || alt(i)-alt(i-1)>1500|| distance(lat(i),lng(i),lat(i-1),lng(i-1))>40|| alt(i-3)>alt(i) && alt(i+3)>alt(i)))&&alt(i)<3000 ){
        cluster_number+=1
        res.append(cluster_number)
      }else{
        res.append(cluster_number)
      }
    }
    res
  }
    def checkTrack(alt:Array[Double]):Boolean={
    var res = 0
    val length=alt.length
    if(length<100) return false
    if(alt(0)<3000){
      res+=1
    }

    if(alt(length-1)<3000){
      res+=1
    }
    if(alt(1)<alt(3) && alt(3)<alt(5)){
      res+=1
    }
    if(alt(length-5)>alt(length-3) && alt(length-3)>alt(length-1)){
      res+=1
    }
//     if(alt(length/2)>10000){
//       res+=1
//     }
    if(res==4) return true
    else return false
  }
  def getFirstElement(time:Array[Double])={
    time(0)
  }
  def getLastElement(time:Array[Double])={
    time(time.length-1)
  }
  def getMaxElement(altitude:Array[Double])={
    val length=altitude.length
    var max=0.0
    for(i<-0 until length){
      if(altitude(i)>max) max= altitude(i)
    }
    max
  }
  
  val MaxElement=udf((altitude:Array[Double])=>getMaxElement(altitude))
  val FirstElement=udf((time:Array[Double])=>getFirstElement(time))
  val LastElement=udf((time:Array[Double])=>getLastElement(time))
  val Cluster_point = udf((timeAtServer:Array[Double],alt:Array[Double],lng:Array[Double],lat:Array[Double]) => getPointCluster(timeAtServer,alt,lng,lat))
  val CheckTrack=udf((alt:Array[Double])=>checkTrack(alt))
}

// COMMAND ----------

val df_feature=df_cls_filter.select($"Icao24",$"cls",Tracker.FirstElement($"timeAtServer_list") as "start",Tracker.LastElement($"timeAtServer_list") as "end",Tracker.FirstElement($"longitude_list") as "start_lon",Tracker.LastElement($"longitude_list") as "end_lon",Tracker.FirstElement($"latitude_list") as "start_lat",Tracker.LastElement($"latitude_list") as "end_lat",Tracker.MaxElement($"altitude_list") as "max_alt")

// COMMAND ----------

// display(df_feature)

// COMMAND ----------

import java.util.function.Function;
object Computer extends Serializable{
  def rad(number:Double): Double ={
    number*Math.PI/180
  }
  def distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = { //返回的是公里为单位的距离数
    val EARTH_RADIUS = 6378.137
    val radLat1 = rad(lat1)
    val radLat2 = rad(lat2)
    val a = rad(lat1) - rad(lat2)
    val b = rad(lng1) - rad(lng2)
    val distance = EARTH_RADIUS * 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin((b) / 2), 2)))
    //    printLog.debug("lat1: " + lat1 + " lng1: " + lng1 + " lat2: " + lat2 + " lng2: " + lng2)
//     println("distance:" + distance)
    distance
  }
  def division(distance:Double,time:Double)={
    distance/time
  }
  
  val Distance=udf((lat1: Double, lng1: Double, lat2: Double, lng2: Double)=>distance(lat1,lng1,lat2,lng2))
  val Division = udf((distance:Double,time:Double)=>division(distance,time))
}

// COMMAND ----------

val df_feature_expand=df_feature.withColumn("distance",Computer.Distance($"start_lat",$"start_lon",$"end_lat",$"end_lon")).withColumn("time",$"end"-$"start").withColumn("avg_speed",Computer.Division($"distance",$"time"))

// COMMAND ----------

// display(df_feature_expand)

// COMMAND ----------

val df_airlines = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/airlines_2.csv").drop("_c3").drop("name").select($"type",$"callsign".alias("Callsign_tempt"))


// COMMAND ----------

val df_map=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_icao_callsign_2017.parquet").select($"Icao".alias("Icao_t"),$"Callsign".alias("Callsign_t"))

// COMMAND ----------

val df_with_callsign=df_feature_expand.join(df_map,df_map("Icao_t")===df_feature_expand("Icao24"),"left")

// COMMAND ----------

val df_test=df_with_callsign.filter($"Callsign_t".isNotNull)


// COMMAND ----------

val df_with_type=df_test.join(df_airlines,df_with_callsign("Callsign_t")===df_airlines("Callsign_tempt"),"left").drop("Callsign_tempt","Icao_t")


// COMMAND ----------

display(df_with_type)

// COMMAND ----------



// COMMAND ----------

// Yiming, find start and end airports of every flight

// Get every airport's location in Eur..
val airports=spark.read.format("csv").option("header", "true").load("dbfs:/FileStore/group16/airports.csv").filter($"type" =!= "heliport" && $"type" =!= "closed").selectExpr("ident as Ident", "name as Name", "round(latitude_deg,1) as latitude", "round(longitude_deg,1) as longtitude")

display(airports)
//def getAirport(lat1: Double, lng1: Double, poi: Array): Array = {
  
//}
def feachairport(lat1:Double, lng1: Double, line : Row) : Unit = {
  var lat2 = line.get(2)
  var lng2 = line.get(3)
  
  return distance(lat1,lng1,lat2,lng2)
  
}

def feachfeature(line : Row) : Unit = {
  
  var lat1 = line.get(6)
  var lng1 = line.get(4)
  var i = 0
  var arr = airports.foreach(feachairport :lat1, lng1, Row => Unit)
  
}

df_feature_expand.foreach(feachfeature : Row => Unit)



// COMMAND ----------

display(airports)

// COMMAND ----------



// COMMAND ----------

