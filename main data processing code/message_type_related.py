# Databricks notebook source
# DBTITLE 1,Rough count of the number of messages of each type
dbutils.fs.ls("/mnt/lsde/opensky/2016")

# COMMAND ----------

# DBTITLE 1,检查 2016 年下的文件路径，选取其中一个进行数据统计
dbutils.fs.ls("/mnt/lsde/opensky/2016/20160921")

# COMMAND ----------

dbutils.fs.ls("/mnt/lsde/opensky/2016/20160918/")

# COMMAND ----------

# DBTITLE 1,选取一个 avro 文件，并粗略显示结构
df = spark.read.format("com.databricks.spark.avro").load("/mnt/lsde/opensky/2016/20160918/part-r-00000-336a9c76-dcd3-4eb2-9cde-32a36dbeba1a.avro")

# COMMAND ----------

df = spark.read.format("com.databricks.spark.avro").load("/mnt/lsde/opensky/2016/*/*.avro").limit(10)

# COMMAND ----------

df.show()

# COMMAND ----------

df = spark.read.option("header",True).csv("/mnt/lsde/opensky/2017/20171012/*.csv/*.gz")

# COMMAND ----------

df.show(truncate=False)

# COMMAND ----------

# DBTITLE 1,提取 rawMessage 的内容
# MAGIC %pip install pyModeS

# COMMAND ----------

from pyModeS import common, adsb, commb, bds
def tell(msg: str) -> dict:
    res={}
    def _print(label, value, unit=None):
        if unit:
            # print(unit)
            res[label] = str(str(value)+" "+unit)
        else:
            res[label] = str(value)
            # print()

    df = common.df(msg)
    icao = common.icao(msg)

    _print("Message", msg)
    _print("ICAO address", icao)
    _print("Downlink Format", df)
    if df==0:
      _print("Protocol", "Short air-air ACAS")
    if df==4:
      _print("Protocol", "Short altitude reply")
    if df==5:
      _print("Protocol", "Short identify reply")
    if df==11:
      _print("Protocol", "All-call reply")
    if df==16:
      _print("Protocol", " Long air-air ACAS")

    if df == 17 or df==18:
        _print("Protocol", "Mode-S Extended Squitter (ADS-B)")
        tc = common.typecode(msg)
        if 1 <= tc <= 4:  # callsign
            _print("Type", "Identification messages")
            callsign = adsb.callsign(msg)
            _print("Callsign:", callsign)

        if 5 <= tc <= 8:  # surface position
            _print("Type", "Surface position messages (including global and local CPR)")


        if 9 <= tc <= 18:  # airborne position
            _print("Type", "Airborne position messages (including global and local CPR)")

        if tc == 19:
            _print("Type", "Airborne velocity messages")

        if tc==28:  # airborne position
            _print("Type", "Aircraft status reports (emergency/priority, TCAS RA)")
        if tc==29:
            _print("Type", "Target state and status messages")
        if tc==31:
            _print("Type", "Operational status reports (airborne and surface)")
      
    if df==19:
      _print("Protocol", "Military Extended Squitter")
    if df == 20:
        _print("Protocol", "Comm-B altitude reply")
#         _print("Altitude", common.altcode(msg), "feet")

    if df == 21:
        _print("Protocol", "Comm-B identify reply")
    if df > 24:
        _print("Protocol", "Comm-D Extended Length Message")

    return res

# COMMAND ----------

# DBTITLE 1,对一列数据的解析，并转换为 DataFrame（暂时用不上）
row=tell("8d70605c990d8c14b00428000000")

row=tell("8d4840d6202cc371c32ce0576098")
# row=tell("02e19690e94ab2")
print(row)

# COMMAND ----------

from pyspark.sql.types import StructField, StringType,StructType
schema = StructType([
        # StructField("timeAtServer", StringType()),
        # StructField("rawMessage",StringType()),
        StructField("Type",StringType()),
        StructField("Protocol",StringType())
        ])

# COMMAND ----------

sc.parallelize([row])

# COMMAND ----------

df = spark.read.json(sc.parallelize([row]),schema=schema)

# COMMAND ----------

df.show()

# COMMAND ----------

# DBTITLE 1,多列解析
import pyModeS as pms

def message_decoder(row):
  row_dict = row.asDict()
  msg = row_dict['rawMessage']
  res = tell(msg)
  for i in res.items():
    row_dict[i[0]] = i[1]
  return row_dict

# COMMAND ----------

df_rdd=df.rdd

# COMMAND ----------

# row_rdd=df_rdd.collect()[1]

# COMMAND ----------

# row_dict=row_rdd.asDict()
# print(row_dict)
# print(row_dict['rawMessage'])

# COMMAND ----------

df_decoded=df_rdd.map(lambda row: message_decoder(row))

# COMMAND ----------

from pyspark.sql.types import StructField, StringType
from pyspark.sql.types import StructType


# COMMAND ----------

schema = StructType([
#         StructField("timeAtServer", StringType()),
#         StructField("rawMessage",StringType()),
#         StructField("Vertical rate",StringType()),
        StructField("Type",StringType()),
#         StructField("Callsign",StringType()),
#         StructField("Track",StringType()),
#         StructField("Speed",StringType()),
        StructField("Protocol",StringType())
#         StructField("ICAO address", StringType()),
#         StructField("Downlink Format", StringType()),
#         StructField("CPR Latitude", StringType()),
#         StructField("CPR Longitude",StringType()),
#         StructField("CPR format", StringType()),
#         StructField("Altitude",StringType())
        ])

# COMMAND ----------

df_schema_changed = spark.createDataFrame(df_decoded,schema=schema)

# COMMAND ----------

df_schema_changed.show(100)


# COMMAND ----------

# df_schema_changed.select(df_schema_changed[0].isNotNull().alias("Type")).groupBy('Type').count().display()
df_schema_changed.groupby("Protocol").count().show(truncate=False)
df_schema_changed.groupby("Type").count().show(truncate=False)
# df_schema_changed.groupby("Callsign").count().show(truncate=False)

# COMMAND ----------

df_schema_changed.select(df_schema_changed[1].isNotNull().alias("Callsign_not_null")).groupBy('Callsign_not_null').count().display()

# COMMAND ----------

# df_adsb_analysis = df_schema_changed.where(df_schema_changed['Protocol']=='Mode-S Extended Squitter (ADS-B)').groupby('Type').count().show(truncate=False)

# COMMAND ----------

df_adsb_analysis.show()

# COMMAND ----------

# DBTITLE 1,单个ICAO，找对应的航司
icao=tell("a800149ec65632b0a80187fa5e1e")['ICAO address']
print(icao)

# COMMAND ----------

import urllib
def request(icao):
    """
    @Param icao type string
    """
    contents = urllib.request.urlopen(
        "https://opensky-network.org/api/states/all?extended=true&icao24="+icao).read()
    print(contents)

# COMMAND ----------

request(icao)

# COMMAND ----------



# COMMAND ----------

row=tell("8D70605C990D8C14B00428000000")

# COMMAND ----------

row

# COMMAND ----------

