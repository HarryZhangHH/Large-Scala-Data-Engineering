# Databricks notebook source
# DBTITLE 1,For the exact size of statistical data sets
import os

# COMMAND ----------

def get_file_path_by_name(file_dir):
    '''
    获取指定路径下所有文件的绝对路径
    :param file_dir:
    :return:
    '''
    L = []
    for root, dirs, files in os.walk(file_dir):  # 获取所有文件
        for file in files:  # 遍历所有文件名
              t=os.path.join(root, file)
              t=t.replace(t[:6],"dbfs:/")
              L.append(t)  # 拼接处绝对路径并放入列表
    print('总文件数目：', len(L))
    return L


# COMMAND ----------

file_path_list_2016=get_file_path_by_name("/dbfs/mnt/lsde/opensky/2016")
total_size_2016=0
count=0
for i in file_path_list_2016:
  count=count+1
  if count%100==0:
    print(count)
  total_size_2016=total_size_2016+dbutils.fs.ls(i)[0].size
print(total_size_2016)

# COMMAND ----------

file_path_list_2017=get_file_path_by_name("/dbfs/mnt/lsde/opensky/2017")
total_size_2017=0
count=0
for i in file_path_list_2017:
  count=count+1
  if count%100==0:
    print(count)
  total_size_2017=total_size_2017+dbutils.fs.ls(i)[0].size
print(total_size_2017)

# COMMAND ----------

