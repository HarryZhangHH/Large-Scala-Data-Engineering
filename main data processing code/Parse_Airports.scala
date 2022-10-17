// Databricks notebook source
// DBTITLE 1,Assigning departure and landing airports to each route and assigning whether it is passenger or cargo based on the classification
import org.opensky.libadsb._
import org.opensky.libadsb.msgs._
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.functions._

// COMMAND ----------

val df = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/airports.csv")//.drop("_c3").drop("name").select($"type",$"callsign".alias("Callsign_tempt"))
// println(df.count())

// COMMAND ----------

val df_drop_test=df.filter($"type" =!="closed" && $"type"=!="heliport").filter($"continent"==="EU").select($"name",$"latitude_deg" as "lat",$"longitude_deg" as "lon")
// println(df_drop_test.count())

// COMMAND ----------

val df_airports=df_drop_test.selectExpr("name","round(lat,2) as lat","round(lon,2) as lon")//.withColumn("id",$"1")

// COMMAND ----------

def add_index(name:String)={
  "1"
}
val AddIndex=udf((name:String)=>add_index(name))

// COMMAND ----------

val df_airports_index=df_airports.withColumn("id",AddIndex($"name")).groupBy($"id").agg(collect_list($"lat") as "lat_list",collect_list($"lon") as "lon_list",collect_list($"name") as "name_list" )

// COMMAND ----------

// val df_flights = spark.read.format("csv").option("header","true")
//                 .load("dbfs:/FileStore/group16/features_with_labels_2016.csv").select("Icao24","cls","start_lon","end_lon","start_lat","end_lat").withColumn("id",AddIndex($"Icao24"))
val df_flights = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/features_2017_pred_labels.csv").select("Icao24","cls","start_lon","end_lon","start_lat","end_lat").withColumn("id",AddIndex($"Icao24"))

// COMMAND ----------

val df_start=df_flights.selectExpr("Icao24 as Icao24_s","cls as cls_s","round(start_lon,2) as start_lon","round(start_lat,2) as start_lat","id as id_s")
val df_end=df_flights.selectExpr("Icao24 as Icao24_e","cls as cls_e","round(end_lon,2) as end_lon","round(end_lat,2) as end_lat","id as id_e")

// COMMAND ----------

val df_start_air=df_start.join(df_airports_index, df_start("id_s")===df_airports_index("id"),"leftouter")
val df_end_air=df_end.join(df_airports_index, df_end("id_e")===df_airports_index("id"),"leftouter")
// display(df_start_air)

// COMMAND ----------

object Finder extends Serializable{
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
  def getName(lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])={
    val length=lat_list.length
    var min=1000000.0
    var index= -1
    for(i<-0 until length){
      val dis_t=distance(lat,lon,lat_list(i),lon_list(i))
      if(dis_t<min){
        min=dis_t
        index=i
      }
    }
    name_list(index)                     
  }
  val GetName=udf((lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])=>getName(lon,lat,lat_list,lon_list,name_list))
  
  def min_distance(lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])={
    val length=lat_list.length
    var min=1000000.0
    var index= -1
    for(i<-0 until length){
      val dis_t=distance(lat,lon,lat_list(i),lon_list(i))
      if(dis_t<min){
        min=dis_t
        index=i
      }
    }
    min                    
  }
  val MinDistance=udf((lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])=>min_distance(lon,lat,lat_list,lon_list,name_list))
  
  
  def getLat(lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])={
    val length=lat_list.length
    var min=1000000.0
    var index= -1
    for(i<-0 until length){
      val dis_t=distance(lat,lon,lat_list(i),lon_list(i))
      if(dis_t<min){
        min=dis_t
        index=i
      }
    }
    lat_list(index)                     
  }
  val GetLat=udf((lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])=>getLat(lon,lat,lat_list,lon_list,name_list))
  
  def getLon(lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])={
    val length=lat_list.length
    var min=1000000.0
    var index= -1
    for(i<-0 until length){
      val dis_t=distance(lat,lon,lat_list(i),lon_list(i))
      if(dis_t<min){
        min=dis_t
        index=i
      }
    }
    lon_list(index)                     
  }
  val GetLon=udf((lon:Double,lat:Double,lat_list:Array[Double],lon_list:Array[Double],name_list:Array[String])=>getLon(lon,lat,lat_list,lon_list,name_list))
  
}

// COMMAND ----------

val df_ports_s=df_start_air.select($"Icao24_s" as "Icao24",$"cls_s" as "cls",Finder.MinDistance($"start_lon",$"start_lat",$"lat_list",$"lon_list",$"name_list") as "MinDistance",Finder.GetName($"start_lon",$"start_lat",$"lat_list",$"lon_list",$"name_list") as "Name",Finder.GetLat($"start_lon",$"start_lat",$"lat_list",$"lon_list",$"name_list") as "s_airports_lat",Finder.GetLon($"start_lon",$"start_lat",$"lat_list",$"lon_list",$"name_list") as "s_airports_lon").withColumnRenamed("Icao24","Icao24_s").withColumnRenamed("cls","cls_s").withColumnRenamed("MinDistance","MinDistance_s").withColumnRenamed("Name","Name_s")

val df_ports_e=df_end_air.select($"Icao24_e" as "Icao24",$"cls_e" as "cls",Finder.MinDistance($"end_lon",$"end_lat",$"lat_list",$"lon_list",$"name_list") as "MinDistance",Finder.GetName($"end_lon",$"end_lat",$"lat_list",$"lon_list",$"name_list") as "Name",Finder.GetLat($"end_lon",$"end_lat",$"lat_list",$"lon_list",$"name_list") as "e_airports_lat",Finder.GetLon($"end_lon",$"end_lat",$"lat_list",$"lon_list",$"name_list") as "e_airports_lon").withColumnRenamed("Icao24","Icao24_e").withColumnRenamed("cls","cls_e").withColumnRenamed("MinDistance","MinDistance_e").withColumnRenamed("Name","Name_e")

// COMMAND ----------

val df_joined=df_ports_s.join(df_ports_e,df_ports_s("Icao24_s")===df_ports_e("Icao24_e") &&df_ports_s("cls_s")===df_ports_e("cls_e")).drop("Icao24_e","cls_e").withColumnRenamed("Icao24_s","Icao24").withColumnRenamed("cls_s","cls")

// COMMAND ----------

// display(df_joined)

// COMMAND ----------

// DBTITLE 1,增加 type
// val df_airlines = spark.read.format("csv").option("header","true")
//                 .load("dbfs:/FileStore/group16/airlines_2.csv").drop("_c3").drop("name").select($"type",$"callsign".alias("Callsign_tempt")) // 这个是确定的type ，以后可能更换
val df_airlines = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/features_2017_pred_labels.csv").dropDuplicates("Callsign_t").select($"type",$"Callsign_t".alias("Callsign_tempt"))
val df_map=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_icao_callsign_2017.parquet").select($"Icao".alias("Icao_t"),$"Callsign".alias("Callsign_t"))

// COMMAND ----------

val df_with_callsign=df_joined.join(df_map,df_map("Icao_t")===df_joined("Icao24"),"left")
val df_test=df_with_callsign.filter($"Callsign_t".isNotNull)


// COMMAND ----------

val df_with_type=df_test.join(df_airlines,df_with_callsign("Callsign_t")===df_airlines("Callsign_tempt"),"left").drop("Callsign_tempt","Icao_t").withColumnRenamed("Callsign_t","Callsign").filter($"type".isNotNull)
//最后一步删除 type=null，以后可能会改

// COMMAND ----------

// display(df_with_type)

// COMMAND ----------

// DBTITLE 1,匹配起降坐标
val features_with_labels=spark.read.option("header", "true").csv("dbfs:/FileStore/group16/features_2017_pred_labels.csv").selectExpr("Icao24 as Icao24_t","cls as cls_t","start","end")

// COMMAND ----------

val se_position=df_with_type.join(features_with_labels,features_with_labels("Icao24_t")===df_with_type("Icao24") &&features_with_labels("cls_t")===df_with_type("cls")).drop("Icao24_t","cls_t")

// COMMAND ----------

display(se_position)

// COMMAND ----------

// DBTITLE 1,统计飞机场的频率
val df_fre=se_position.groupBy("Name_e","Name_s").agg(sum($"type") as "Addition",count($"type") as "num").withColumn("frequency",$"Addition"/$"num").filter($"Addition"=!=0) //频率

// COMMAND ----------

display(df_fre)

// COMMAND ----------

