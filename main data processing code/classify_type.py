#!/usr/bin/env python
# coding: utf-8

# In[41]:


from astral import LocationInfo
from astral.sun import sun
import datetime
import numpy as np
import pandas as pd
from sklearn.utils import shuffle
import numpy as np
from sklearn import svm, metrics
from sklearn.model_selection import train_test_split
from matplotlib import pyplot as plt
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import roc_curve, confusion_matrix, plot_confusion_matrix
from sklearn.preprocessing import MinMaxScaler
import math

def gettimearea(timestamp, longtitude, latitude):
#     print(timestamp.shape)
    time_string = datetime.datetime.fromtimestamp(timestamp)
    year = time_string.year
    month = time_string.month
    day = time_string.day
    timearea = 0
    try:
        city = LocationInfo("", "", "UTC", latitude, longtitude)
        s = sun(city.observer, date=datetime.date(year, month, day))
        sunrise = s["sunrise"].replace(tzinfo=None)
        sunset = s["sunset"].replace(tzinfo=None)
        if sunrise < time_string <= sunset:
            timearea = 1
        else:
            timearea = 0
    except:
        timearea = None
    return timearea

def gettimedifference(timestamp, longtitude, latitude, n):
#     print(timestamp.shape)
    time_string = datetime.datetime.fromtimestamp(timestamp)
    year = time_string.year
    month = time_string.month
    day = time_string.day
    timedifference = 0
    try:
        city = LocationInfo("", "", "UTC", latitude, longtitude)
        s = sun(city.observer, date=datetime.date(year, month, day))
        sunrise = s["sunrise"].replace(tzinfo=None)
        sunset = s["sunset"].replace(tzinfo=None)
        if n == 0:
            timedifference = time_string - sunrise
        else:
            timedifference = time_string - sunset
    except:
        timedifference = None
    return timedifference

def data_preprocessing(df, n):
    df2 = df.drop(['cls', 'Callsign_t', 'start', 'end', 'start_lon', 'end_lon', 'start_lat', 'end_lat'],axis=1)
    df2 = df2.dropna()
    df3 = df2[df2['type']==0].drop_duplicates('Icao24')
    df3 = shuffle(df3)
    df3 = df3.reset_index()
    df4 = df3.loc[:n,:]
#     df5 = df3.loc[n:,:]
    dftrain = pd.concat([df2[df2['type']==1], df4]).reset_index().drop(['index'], axis=1)
    dftrain = shuffle(dftrain)
    dflabel = dftrain['type']
    dftrain = dftrain.drop(['level_0', 'Icao24', 'type'], axis=1)
    train = np.array(dftrain)
    label = np.array(dflabel)
    print(train.shape)
    train, label = find_anomalies(train, label)
    return train, label

# anomaly detection
def find_anomalies(dataset, label):
    #define a list to accumlate anomalies
    anomalies = []
    train = []
    label_2 = []
    lower = []
    upper = []
    lower_2 = []
    upper_2 = []
    datasetT = dataset.T
    # Set upper and lower limit to 3 standard deviation
    for data in datasetT:
        data_std = np.std(data)
        data_mean = np.mean(data)
        anomaly_cut_off = data_std * 3

        lower_limit  = data_mean - anomaly_cut_off 
        upper_limit = data_mean + anomaly_cut_off
        anomaly_cut_off_2 = data_std * 2

        lower_limit_2  = data_mean - anomaly_cut_off_2
        upper_limit_2 = data_mean + anomaly_cut_off_2
        lower.append(lower_limit)
        upper.append(upper_limit)
        lower_2.append(lower_limit_2)
        upper_2.append(upper_limit_2)
    # Generate outliers
    for index, outlier in enumerate(dataset):
        train_value = []
        for i, value in enumerate(outlier):
            if value > upper[i] or value < lower[i]:
                anomalies.append(outlier)
                break
            elif upper[i] >= value > upper_2[i]:
                train_value.append(upper_2[i])
            elif lower[i] <= value < lower_2[i]:
                train_value.append(lower_2[i])
            else: 
                train_value.append(value)
        if len(train_value) == 10:
            train.append(train_value)
            label_2.append(label[index])
    dtrain = np.array(train, dtype=np.float32)
    dlabel_2 = np.asarray(label_2)
    print(dtrain.shape)
    return dtrain, dlabel_2

# machine learning model
def random_forest(X_train, X_test, y_train, y_test, k):
    # normalize
    scaler = MinMaxScaler()
    scaler.fit(X_train)
    scaler.fit(X_test)
    forest = RandomForestClassifier(n_estimators=10000, random_state=0)
    forest.fit(X_train, y_train)
    predictions = forest.predict(X_test)
    acc = metrics.accuracy_score(y_test, predictions)
    f1_score = metrics.f1_score(y_test, predictions)
    print("Accuray: {}".format(acc))
    print("F1-score: {}".format(f1_score))
    print("Feature importance:\n{}".format(forest.feature_importances_))
#     plot_feature_importance(forest)
    fpr, tpr , thresholds = roc_curve(y_test, forest.predict_proba(X_test)[:, 1])
    label_name = "ROC Curve"+str(k)
    plt.plot(fpr, tpr, label=label_name)
    
    plt.xlabel("FDR")
    plt.ylabel("TPR")
    close_default = np.argmin(np.abs(thresholds - 0.5))
    if k == 2.5:
        plt.plot(fpr[close_default], tpr[close_default], '^', markersize=10, label="threshold 0,5 RF"+str(k), fillstyle="none", c='k', mew=2)
    elif k ==4:
        plt.plot(fpr[close_default], tpr[close_default], 'o', markersize=10, label="threshold 0,5 RF"+str(k), fillstyle="none", c='k', mew=2)
    elif k == 3.5:
        plt.plot(fpr[close_default], tpr[close_default], 'v', markersize=10, label="threshold 0,5 RF"+str(k), fillstyle="none", c='k', mew=2)
    elif k == 3:
        plt.plot(fpr[close_default], tpr[close_default], '.', markersize=10, label="threshold 0,5 RF"+str(k), fillstyle="none", c='k', mew=2)
    else:
        plt.plot(fpr[close_default], tpr[close_default], '+', markersize=10, label="threshold 0,5 RF"+str(k), fillstyle="none", c='k', mew=2)
        
    plt.legend(loc=4)
    
    confusion = confusion_matrix(y_test, predictions)
    print("Confusion matrix:\n{}".format(confusion))
#     plot_confusion_matrix(forest, X_test, y_test)
    return forest

def plot_feature_importance(model):
    label = ["max_alt", "distance", "time", "avg_speed", "startarea", "endarea", "start_sunrise", "start_sunset", "end_sunrise", "end_sunset"]
    n_features = X_train.shape[1]
    plt.barh(range(n_features), model.feature_importances_, align='center')
    plt.yticks(np.arange(n_features), label)
    plt.xlabel("Feature importance")
    plt.ylabel("Feature")
    plt.savefig('feature_importance', bbox_inches='tight', pad_inches=0)
    
if __name__ == '__main__':
    
    df2016 = pd.read_csv('features_with_labels_2016.csv')
    df2016.head()
    df2017 = pd.read_csv('features_with_labels_2017.csv')
    df2017.head()

    n = df2017.groupby('type').count().iloc[1,0]
    n2 = df2016.groupby('type').count().iloc[1,0]
    n3 = df2017.groupby('type').count().iloc[0,0]
    n4 = df2016.groupby('type').count().iloc[0,0]
    r = round(n3/n)
    r2 = round(n4/n2)
    
    # feature increment
    df2017['startarea'] = df2017.apply(lambda row: gettimearea(row['start'], row['start_lon'], row['start_lat']), axis=1)
    df2017['endarea'] = df2017.apply(lambda row: gettimearea(row['end'], row['end_lon'], row['end_lat']), axis=1)
    df2017['start_sunrise'] = df2017.apply(lambda row: gettimedifference(row['start'], row['start_lon'], row['start_lat'], 0), axis=1)
    df2017['start_sunset'] = df2017.apply(lambda row: gettimedifference(row['start'], row['start_lon'], row['start_lat'], 1), axis=1)
    df2017['end_sunrise'] = df2017.apply(lambda row: gettimedifference(row['end'], row['end_lon'], row['end_lat'], 0), axis=1)
    df2017['end_sunset'] = df2017.apply(lambda row: gettimedifference(row['end'], row['end_lon'], row['end_lat'], 1), axis=1)

    df2017['start_sunrise'] = (df2017['start_sunrise']/np.timedelta64(1, 'D')).astype(float)
    df2017['start_sunset'] = (df2017['start_sunset']/np.timedelta64(1, 'D')).astype(float)
    df2017['end_sunrise'] = (df2017['end_sunrise']/np.timedelta64(1, 'D')).astype(float)
    df2017['end_sunset'] = (df2017['end_sunset']/np.timedelta64(1, 'D')).astype(float)

    df2016['startarea'] = df2016.apply(lambda row: gettimearea(row['start'], row['start_lon'], row['start_lat']), axis=1)
    df2016['endarea'] = df2016.apply(lambda row: gettimearea(row['end'], row['end_lon'], row['end_lat']), axis=1)
    df2016['start_sunrise'] = df2016.apply(lambda row: gettimedifference(row['start'], row['start_lon'], row['start_lat'], 0), axis=1)
    df2016['start_sunset'] = df2016.apply(lambda row: gettimedifference(row['start'], row['start_lon'], row['start_lat'], 1), axis=1)
    df2016['end_sunrise'] = df2016.apply(lambda row: gettimedifference(row['end'], row['end_lon'], row['end_lat'], 0), axis=1)
    df2016['end_sunset'] = df2016.apply(lambda row: gettimedifference(row['end'], row['end_lon'], row['end_lat'], 1), axis=1)

    df2016['start_sunrise'] = (df2016['start_sunrise']/np.timedelta64(1, 'D')).astype(float)
    df2016['start_sunset'] = (df2016['start_sunset']/np.timedelta64(1, 'D')).astype(float)
    df2016['end_sunrise'] = (df2016['end_sunrise']/np.timedelta64(1, 'D')).astype(float)
    df2016['end_sunset'] = (df2016['end_sunset']/np.timedelta64(1, 'D')).astype(float)
    
    # k is the ratio of passenger:cargo
    k=3.5
    train2017, label2017 = data_preprocessing(df2017, k*n)
    train2016, label2016 = data_preprocessing(df2016, k*n2)
    X = np.append(train2017, train2016, axis=0)
    y = np.append(label2017, label2016, axis=0)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
    test2017, testlabel2017 = data_preprocessing(df2017, (r-k)*n)
    test2016, testlabel2016 = data_preprocessing(df2016, (r-k)*n2)
    XX = np.append(test2017, test2016, axis=0)
    yy = np.append(testlabel2017, testlabel2016, axis=0)
    _, XX, _, yy = train_test_split(XX, yy, test_size=0.2)
    XX = np.append(X_test, XX, axis=0)
    yy = np.append(y_test, yy, axis=0)
    model = random_forest(X_train, XX, y_train, yy, k)
    plt.savefig('ROC_2', bbox_inches='tight')
    dfvalid = df2017[df2017.isnull().values == True]
    dfvalid = dfvalid.drop(['Icao24', 'cls', 'Callsign_t', 'start', 'end', 'start_lon', 'end_lon', 'start_lat', 'end_lat', 'type'],axis=1)
    dfvalid = dfvalid.dropna()

    valid = np.array(dfvalid)
    predict = model.predict(valid)
    result = pd.DataFrame(predict,columns=['type'])
    result.groupby('type')['type'].count()
    
    dfvalid = dfvalid.reset_index()
    dfpredict = pd.concat([dfvalid, result], axis=1).drop(['startarea','endarea', 'max_alt', 'distance', 'time', 'avg_speed', 'start_sunrise', 'start_sunset', 'end_sunrise', 'end_sunset'], axis=1)

    dfclassified = pd.merge(df2017, dfpredict, left_index=True, right_on='index', how='outer')
    dfclassified['type'] = dfclassified.apply(lambda row: row['type_y'] if math.isnan(row['type_x']) else row['type_x'], axis=1)
    
    dfclassified = dfclassified.drop(['type_x', 'type_y'], axis=1).reset_index()
    dfclassified.to_csv(r'features_2017.csv',encoding='GBK',mode='a')

