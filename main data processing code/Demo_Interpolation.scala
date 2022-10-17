// Databricks notebook source
// DBTITLE 1,An example of interpolation using timestamps
import org.opensky.libadsb._
import org.opensky.libadsb.msgs._
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.functions._

// COMMAND ----------

val df_cls_filter=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_cls_filter_20160918.parquet").drop("quality")

// COMMAND ----------

val df_test=df_cls_filter.select($"Icao24",$"altitude_list",$"timeAtServer_list",$"longitude_list",$"latitude_list",$"cls",
                                     explode(
                                       arrays_zip($"longitude_list",$"latitude_list",$"timeAtServer_list",$"altitude_list")
                                       .cast("array<struct<longitude:Double,latitude:Double,timeAtServer:Double,altitude:Double>>")
                                       ).as("Position")
                                     ).select($"Icao24",$"Position.longitude" as "lon",$"Position.latitude" as "lat",$"Position.altitude" as "alt",$"Position.timeAtServer" as "timeAtsServer",$"cls")

// COMMAND ----------

// display(df_test)

// COMMAND ----------

val df_round=df_test.selectExpr("Icao24","round(lon,4) as lon","round(lat,4) as lat","alt","timeAtsServer as timeAtServer","cls")

// COMMAND ----------

val df_drop=df_round.dropDuplicates("Icao24","lon","lat").orderBy("Icao24","timeAtServer") //如果没有 orderBy,Icao 24的数据不集中，是散乱的

// COMMAND ----------

display(df_drop)

// COMMAND ----------

// DBTITLE 1,手动建立 DataFrame
val df = Seq[(String, Double,Double)](
  ("15409a", 31,0.5E9),
  ("15409a", 37,1.5E9),
  ("15409a", 33,2.1E9),
  ("15409b", 33,0.3E9),
  ("15409b", 34,1.4E9),
  ("15409b", 10,2.6E9),
  ("15409b", 5,0.2E9)
).toDF("Icao24", "lon","time")

// COMMAND ----------

import scala.collection.mutable.ListBuffer
def date_range(t1:Double, t2:Double)={
    val step=0.1E9
    var res=new ListBuffer[Double]
    //Return a list of equally spaced points between t1 and t2 with stepsize step.
    val upper=((t2-t1)/step).toInt
    for(i<-0 to upper){
      res.append(t1+step*i)
    }
    res
//     return [t1 + step*x for x in range(int((t2-t1)/step)+1)]
}

// COMMAND ----------

val date_range_udf = udf((t1:Double,t2:Double)=>date_range(t1,t2))


// COMMAND ----------

// println(date_range(0.5E9,3.5E9))

// COMMAND ----------

val df_base=df.groupBy("Icao24").agg(min($"time").cast("long").alias("readtime_min"),max($"time").cast("long").alias("readtime_max"))

// COMMAND ----------

display(df_base)

// COMMAND ----------

val df_explode=df_base.withColumn("time",explode(date_range_udf($"readtime_min",$"readtime_max"))).drop("readtime_min","readtime_max")

// COMMAND ----------

display(df_explode)

// COMMAND ----------

val df_new_time=df_explode.select($"Icao24" as "new_Icao24",$"time" as "new_time")

// COMMAND ----------

display(df)

// COMMAND ----------

val df_all_dates=df_new_time.join(df,df_new_time("new_time")===df("time") && df_new_time("new_Icao24")===df("Icao24"),"leftouter").drop("Icao24")

// COMMAND ----------

display(df_all_dates)

// COMMAND ----------

import org.apache.spark.sql.expressions.Window
val window_ff = Window.partitionBy("new_Icao24").orderBy("new_time").rowsBetween(-10000, 0)               
val window_bf = Window.partitionBy("new_Icao24").orderBy("new_time").rowsBetween(0, 100000)

// COMMAND ----------

val read_last = df_all_dates.withColumn("readvalue_ff",last($"lon",true) over window_ff).withColumn("readtime_ff",last($"time",true) over window_ff).withColumn("readvalue_bf",first($"lon",true) over window_bf).withColumn("readtime_bf",first($"time",true) over window_bf)

// COMMAND ----------

display(read_last)

// COMMAND ----------

val df_all=read_last.withColumn("new_lon",$"lon".cast("string")).drop("lon")
display(df_all)

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

// COMMAND ----------

display(df_filled)

// COMMAND ----------

