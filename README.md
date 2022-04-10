# Large-Scala-Data-Engineering

# Some data processing pipelines.zip

(Run on Databricks)

Parse_RawMessage_Track: This file is used to parse raw dataset and detect flights

Parse_Callsign: Extract and generate a mapping table of Icao to Callsign

Parse_Airports: Assigning departure and landing airports to each route and assigning whether it is passenger or cargo based on the classification

Parse_Cluster: Comparison of major routes for passenger and freight traffic using clustering algorithms

Parse_Feature: Initial generation of features needed for the classification model

Parse_Interpolation_Vis: Generate the interpolated data needed for the visualisation

Get_Size_of_Folder: For the exact size of statistical data sets

Demo_Interpolation: An example of interpolation using timestamps

message_type_related: Rough count of the number of messages of each type

# Classification Part.zip

(Run locally)

classify_type.py :  Classify the preliminary data using random forest model accompany with feature increment, anomaly detection and normalization. The input is a csv dataset after initial classification and the output is the dataset after classification using the model.

experiment and statistics.zip :  Contains three .ipynb files which are separately experimentation and contrast of the multiple machine learning model, flight feature extraction and data statistic and plot the statistical chart.

# Group16-AirCargo-visualisations.zip

This visualization project uses JQuery to read the json file. When using the Chrome browser, please disable web security of Google Chrome.
MAC:
open /Applications/Google\ Chrome.app/ --args --disable-web-security --user-data-dir

WINDOWS
"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --disable-web-security --user-data-dir="C:/ChromeDevSession"

If you don't want sitting your Browser. You can also see it on [https://lsde-group16.github.io/Air-Cargo/](https://lsde-group16.github.io/Air-Cargo/)
    
