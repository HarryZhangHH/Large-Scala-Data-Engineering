// Databricks notebook source
// DBTITLE 1,Generate the interpolated data needed for the visualisation
import org.opensky.libadsb._
import org.opensky.libadsb.msgs._
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.functions._

// COMMAND ----------

val df_cls_filter=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_cls_filter_2017_2.parquet").drop("quality")
// val df_cls_filter=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_cls_filter_2016.parquet").drop("quality")

// COMMAND ----------

val df_test=df_cls_filter.select($"Icao24",$"altitude_list",$"timeAtServer_list",$"longitude_list",$"latitude_list",$"cls",
                                     explode(
                                       arrays_zip($"longitude_list",$"latitude_list",$"timeAtServer_list",$"altitude_list")
                                       .cast("array<struct<longitude:Double,latitude:Double,timeAtServer:Double,altitude:Double>>")
                                       ).as("Position")
                                     ).select($"Icao24",$"Position.longitude" as "lon",$"Position.latitude" as "lat",$"Position.altitude" as "alt",$"Position.timeAtServer" as "timeAtsServer",$"cls")

// COMMAND ----------

val df_round=df_test.selectExpr("Icao24","lon","lat","alt","round(timeAtsServer/1000,0) as timeAtServer","cls")

// COMMAND ----------

val df_drop=df_round.dropDuplicates("Icao24","timeAtServer","cls").orderBy("Icao24","timeAtServer") //如果没有 orderBy,Icao 24的数据不集中，是散乱的

// COMMAND ----------

// display(df_drop.groupBy("Icao24","cls").count())

// COMMAND ----------

import scala.collection.mutable.ListBuffer
def date_range(t1:Double, t2:Double)={
    val step=1
    var res=new ListBuffer[Double]
    //Return a list of equally spaced points between t1 and t2 with stepsize step.
    val upper=((t2-t1)/step).toInt
    for(i<-0 to upper){
      res.append(t1+step*i)
    }
    res
}
val date_range_udf = udf((t1:Double,t2:Double)=>date_range(t1,t2))

// COMMAND ----------

val df_base=df_drop.groupBy("Icao24","cls").agg(min($"timeAtServer").cast("long").alias("readtime_min"),max($"timeAtServer").cast("long").alias("readtime_max"))

// COMMAND ----------

val df_explode=df_base.withColumn("time",explode(date_range_udf($"readtime_min",$"readtime_max"))).drop("readtime_min","readtime_max")

// COMMAND ----------

val df_new_time=df_explode.select($"Icao24" as "new_Icao24",$"time" as "new_time",$"cls" as "new_cls")

// COMMAND ----------

val df_all_dates=df_new_time.join(df_drop,df_new_time("new_time")===df_drop("timeAtServer") && df_new_time("new_Icao24")===df_drop("Icao24") && df_new_time("new_cls")===df_drop("cls"),"leftouter").drop("Icao24").drop("cls")

// COMMAND ----------

import org.apache.spark.sql.expressions.Window
val window_ff = Window.partitionBy("new_Icao24","new_cls").orderBy("new_time").rowsBetween(-1000000, 0)               
val window_bf = Window.partitionBy("new_Icao24","new_cls").orderBy("new_time").rowsBetween(0, 10000000)

// COMMAND ----------

val read_last = df_all_dates.withColumn("readvalue_ff",last($"lon",true) over window_ff).withColumn("readtime_ff",last($"timeAtServer",true) over window_ff).withColumn("readvalue_bf",first($"lon",true) over window_bf).withColumn("readtime_bf",first($"timeAtServer",true) over window_bf)
.withColumn("readvalue_ff_lat",last($"lat",true) over window_ff).withColumn("readtime_ff_lat",last($"timeAtServer",true) over window_ff).withColumn("readvalue_bf_lat",first($"lat",true) over window_bf).withColumn("readtime_bf_lat",first($"timeAtServer",true) over window_bf)
.withColumn("readvalue_ff_alt",last($"alt",true) over window_ff).withColumn("readtime_ff_alt",last($"timeAtServer",true) over window_ff).withColumn("readvalue_bf_alt",first($"alt",true) over window_bf).withColumn("readtime_bf_alt",first($"timeAtServer",true) over window_bf)

// COMMAND ----------

val df_all=read_last.withColumn("new_lon",$"lon".cast("string")).drop("lon")
.withColumn("new_lat",$"lat".cast("string")).drop("lat")
.withColumn("new_alt",$"alt".cast("string")).drop("alt")

// COMMAND ----------

def interpol(x:Double,x_prev:Double,x_next:Double,y_prev:Double,y_next:Double,y:String)={
  if(x_prev==x_next){
    y
  }else{
    val m = (y_next-y_prev)/(x_next-x_prev)
    val y_interpol = y_prev + m * (x - x_prev)
    y_interpol.toString
  }
}

// COMMAND ----------

val interpol_udf = udf((x:Double,x_prev:Double,x_next:Double,y_prev:Double,y_next:Double,y:String)=>interpol(x,x_prev,x_next,y_prev,y_next,y))


// COMMAND ----------

val df_filled = read_last.withColumn("readvalue_interpol", interpol_udf($"new_time", $"readtime_ff", $"readtime_bf", $"readvalue_ff", $"readvalue_bf", $"lon"))
.withColumn("readvalue_interpol_lat", interpol_udf($"new_time", $"readtime_ff_lat", $"readtime_bf_lat", $"readvalue_ff_lat", $"readvalue_bf_lat", $"lat"))
.withColumn("readvalue_interpol_alt", interpol_udf($"new_time", $"readtime_ff_alt", $"readtime_bf_alt", $"readvalue_ff_alt", $"readvalue_bf_lat", $"alt"))


// COMMAND ----------

// display(df_filled)

// COMMAND ----------



// COMMAND ----------

val df_select=df_filled.select($"new_Icao24".alias("Icao24"),$"new_time".alias("timeAtServer"),$"readvalue_interpol".alias("lon"),$"readvalue_interpol_lat".alias("lat"),$"readvalue_interpol_alt".alias("alt"),$"new_cls".alias("cls"))

// COMMAND ----------

// display(df_select)

// COMMAND ----------

// display(df_select.groupBy("Icao24","cls").count())

// COMMAND ----------



// COMMAND ----------

// val df_airlines = spark.read.format("csv").option("header","true")
//                 .load("dbfs:/FileStore/group16/airlines_2.csv").drop("_c3").drop("name").select($"type",$"callsign".alias("Callsign_tempt"))

val df_airlines = spark.read.format("csv").option("header","true")
                .load("dbfs:/FileStore/group16/2017_airports_with_position.csv").dropDuplicates("callsign").select($"type",$"callsign".alias("Callsign_tempt"))
val df_map=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_icao_callsign_2017.parquet").select($"Icao".alias("Icao_t"),$"Callsign".alias("Callsign_t"))

// COMMAND ----------

val df_with_callsign=df_select.join(df_map,df_map("Icao_t")===df_select("Icao24"),"left")
// display(df_with_callsign)

// COMMAND ----------

val df_test=df_with_callsign.filter($"Callsign_t".isNotNull)
// display(df_test)

// COMMAND ----------

val df_with_type=df_test.join(df_airlines,df_with_callsign("Callsign_t")===df_airlines("Callsign_tempt"),"left").filter($"Callsign_tempt".isNotNull).drop("Callsign_tempt","Icao_t")
// display(df_with_type)

// COMMAND ----------

val df_vis=df_with_type.selectExpr("Icao24","timeAtServer*1000 as timeAtServer","round(lon,4) as lon","round(lat,4) as lat","round(alt,4) as alt","cls","Callsign_t as Callsign","type")

// COMMAND ----------

display(df_vis)

// COMMAND ----------

df_vis.write.option("header", "true").csv("dbfs:/mnt/group16/vis_1000_2017_2.csv")

// COMMAND ----------

// val df_cls_res=  spark.read.format("csv").option("header","true")
//                 .load("dbfs:/FileStore/group16/features_2017_pred_labels.csv").select($"Icao24" as "Icao24_T",$"cls" as "cls_T",$"type" as "type_T")

// COMMAND ----------

// val df_with_res=df_vis.join(df_cls_res,df_cls_res("Icao24_T")===df_vis("Icao24")&&df_cls_res("cls_T")===df_vis("cls"),"left")

// COMMAND ----------

// val df_out=df_with_res.drop("Icao24_T","cls_T","type").withColumnRenamed("type_T","type")

// COMMAND ----------

// display(df_out)

// COMMAND ----------

// df_vis.write.parquet("dbfs:/mnt/group16/df_vis_2016.parquet")

// COMMAND ----------

// display(df_vis)

// COMMAND ----------

// display(df_vis.groupBy("Icao24","cls").count())

// COMMAND ----------

// val df_t=df_with_type.dropDuplicates("Icao24","type","cls")

// COMMAND ----------

// val df_filted=df_t.filter($"type".isNotNull)

// COMMAND ----------

// display(df_filted)

// COMMAND ----------

