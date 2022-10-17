var ctx = {
    w: 960,
    h: 500,
    //w: d3.select("#main").attr("width"),
    //h: d3.select("#main").attr("height")
    i: 0,
    LA_MIN: 41.31,
    LA_MAX: 51.16,
    LO_MIN: -4.93,
    LO_MAX: 7.72,
    TRANSITION_DURATION: 1000,
    timestamp: 1474157000,
    scale: 1,
    currentFlights: [],
    data1: [],
    data2: [],
    planeUpdater: null,
    year: 2016
};

const PROJECTIONS = {
    ER: d3.geoEquirectangular().center([-10,20]).scale(1200).translate([200,1000]),
    //ER: d3.geoEquirectangular().center([0,0]).scale(128).translate([ctx.w/2,ctx.h/2]),
};
var path4proj = d3.geoPath()
                  .projection(PROJECTIONS.ER);

var drawMap = function(countries, lakes, rivers, svgEl){

    ctx.mapG = svgEl.append("g")
                    .attr("id", "map");
    // bind and draw geographical features to <path> elements
    var path4proj = d3.geoPath()
                 .projection(PROJECTIONS.ER);
    var countryG = ctx.mapG.append("g").attr("id", "countries");
    countryG.selectAll("path.country")
            .data(countries.features)
            .enter()
            .append("path")
            .attr("d", path4proj)
            .attr("class", "country");
    var lakeG = ctx.mapG.append("g").attr("id", "lakes");
    lakeG.selectAll("path.lakes")
         .data(lakes.features)
         .enter()
         .append("path")
         .attr("d", path4proj)
         .attr("class", "lake");
    var riverG = ctx.mapG.append("g").attr("id", "rivers");
    riverG.selectAll("path.rivers")
         .data(rivers.features)
         .enter()
         .append("path")
         .attr("d", path4proj)
         .attr("class", "river");
    ctx.mapG.append("g")
            .attr("id", "planes");
    // pan & zoom
    function zoomed(event, d) {
      //ctx.mapG.attr("transform", event.transform);
      ctx.mapG.attr("transform" , event.transform);
      var scale = ctx.mapG.attr("transform");
      scale = scale.substring(scale.indexOf('scale(')+6);
      scale = parseFloat(scale.substring(0, scale.indexOf(')')));
      ctx.scale = 1 / scale;
      if (ctx.scale != 1){
          d3.selectAll("image")
            .attr("transform", (d) => (getPlaneTransform(d)));
      }
    }
    var zoom = d3.zoom()
        .scaleExtent([1, 40])
        .on("zoom", zoomed);

    svgEl.call(zoom);

    //Scrubber([1,2,3,4,5,6,7,8]);

};
var svgEl = d3.select("#main").append("svg");;
var createViz = function(){
    //d3.time.scale()
    d3.select("body")
      .on("keydown", function(data, event,d){drawPlot2(data,event);});
    //svgEl = svgEl = d3.select("#main").append("svg");
    svgEl.attr("width", ctx.w);
    svgEl.attr("height", ctx.h);
    svgEl.append("rect")
         .attr("x", 0)
         .attr("y", 0)
         .attr("width", "100%")
         .attr("height", "100%")
         .attr("fill", "#bcd1f1");

    loadGeo(svgEl);
    drawpassenger();
    //altitudeBar();
    drawcargo();
    //speedBar();
    //loadSli(svgEl);
    //Scrubber([1,2,3,4,5,6,7,8]);
};


/* data fetching and transforming */
var loadGeo = function(svgEl){
    var promises = [//d3.json("ne_50m_admin_0_countries.geojson"),
                    $.getJSON("ne_50m_admin_0_countries.geojson"),
                    $.getJSON("ne_50m_lakes.geojson"),
                    $.getJSON("ne_50m_rivers_lake_centerlines.geojson")
                    //d3.json("ne_50m_lakes.geojson"),
                    //d3.json("ne_50m_rivers_lake_centerlines.geojson")
                  ];
    Promise.all(promises).then(function(data){
        drawMap(data[0], data[1], data[2], svgEl);
    }).catch(function(error){console.log(error)});

};

function formatTimestamp(timestamp) {
    //if (sliderDateFormat === undefined) {
    sliderDateFormat = new Date();
    //}
    sliderDateFormat.setTime(timestamp * 1000);
    return sliderDateFormat.toUTCString();
}

function getExactTime(time) {
        var date = new Date(time);
        // var date = new Date(time* 1000);
        var year = date.getFullYear() + '-';
        var month = (date.getMonth()+1 < 10 ? '0' + (date.getMonth()+1) : date.getMonth()+1) + '-';
        var dates = date.getDate() + ' ';
        var hour = date.getHours() + ':';
        var min = date.getMinutes() + ':';
        var second = date.getSeconds();
        return year + month + dates + hour + min + second ;
 }


//var formatDateIntoYear = d3.timeFormat("%Y");
//var formatDate = d3.timeFormat("%d %b");
//var parseDate = d3.timeParse("%m/%d/%y");

//var startDate = new Date(formatTimestamp(1474239711)),
//    endDate = new Date(formatTimestamp(1474239819));
//GetVal();
//if(document.getElementById("year").value === "2016"){
//  var startDate = 1474157000,
//      endDate =   1474759000;
//}
//else{
//  var startDate = 1507421000,
//      endDate =   1508026000;
//}
var startDate = 1474157000,
    endDate =   1474759000;

var margin = {top:50, right:10, bottom:0, left:80},
    width = 1400 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom;

var svg = d3.select("#vis")
    .append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height/4 + margin.top + margin.bottom);

////////// slider //////////

var moving = false;
var currentValue = 0;
var targetValue = width;

var playButton = d3.select("#play-button");

//var x = d3.scaleTime()
//    .domain([startDate, endDate])
//    .range([0, targetValue])
//    .clamp(true);
var parseDate = d3.timeParse("%m/%d/%y");

var x = d3.scaleLinear()
        .domain([startDate, endDate])
        .range([0, targetValue])
        .clamp(true);

var slider = svg.append("g")
    .attr("class", "slider")
    .attr("transform", "translate(" + margin.left + "," + height/5 + ")");

slider.append("line")
    .attr("class", "track")
    .attr("x1", x.range()[0])
    .attr("x2", x.range()[1])
  .select(function() { return this.parentNode.appendChild(this.cloneNode(true)); })
    .attr("class", "track-inset")
  .select(function() { return this.parentNode.appendChild(this.cloneNode(true)); })
    .attr("class", "track-overlay")
    .call(d3.drag()
        .on("start.interrupt", function() { slider.interrupt(); })
        .on("start drag", function() {
          currentValue = Math.floor(event.x - 80) ;
          update(x.invert(currentValue));
        })
    );

slider.insert("g", ".track-overlay")
    .attr("class", "ticks")
    .attr("transform", "translate(0," + 18 + ")")
  .selectAll("text")
    .data(x.ticks(10))
    .enter()
    .append("text")
    .attr("x", x)
    .attr("y", 10)
    .attr("text-anchor", "middle")
    .text(function(d) { return d; });

var handle = slider.insert("circle", ".track-overlay")
    .attr("class", "handle")
    .attr("r", 9);

var label = slider.append("text")
    .attr("class", "label")
    .attr("text-anchor", "middle")
    .text(startDate)
    .attr("transform", "translate(0," + (-25) + ")")


////////// plot //////////

var dataset;
var newData;

var plot = svg.append("g")
    .attr("class", "plot")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


//d3.csv("./data/test.csv", prepare, function(data) {
//  dataset = data;
  //console.log(dataset);
//  drawPlot(dataset);

playButton
    .on("click", function() {
    var button = d3.select(this);
    if (button.text() == "Pause") {
      moving = false;
      clearInterval(timer);
      // timer = 0;
      button.text("Play");
    } else {
      moving = true;
      timer = setInterval(step, 0);
      button.text("Pause");
      initflight();
      drawflight2(ctx.data1);
      //drawflight();
      drawcargo();
      drawpassenger();
      //altitudeBar();
      }
    console.log("Slider moving: " + moving);
})
//})

function GetVal(){
    ctx.year = document.getElementById("year").value;
    console.log(ctx.year);
}

var initflight = function() {
    //GetVal();
    //console.log(document.getElementById("year").value === "2016")
    //if(document.getElementById("year").value === "2016"){
    //    d3.json("./data/vis_1000_2016.json").then((data) => {
    //        ctx.data1 = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
    //      });
    //}
    //else if (document.getElementById("year").value === "2017") {
    //  d3.json("./data/vis_1000_2017.json").then((data) => {
    //      ctx.data1 = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
      //  });
    //};
    //d3.json("./data/1000_2016.json").then((data) => {
    $.getJSON("./data/1000_2016.json", function (data){

        ctx.data1 = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
    });
    d3.selectAll("image").remove();
    //ctx.data1 = datas;

}
var color = ["./pic/Passenger.png","./pic/Cargo3.png"];
var size = [5,8];

function drawflight2(data, e){
    ctx.currentFlights = data;
    const planes = d3.select("g #planes").selectAll("image").data(ctx.currentFlights);

    var exit = planes.exit();

    const images = planes.enter().append("image");
    //d3.selectAll("image").remove();

    images.attr("class", (d, i) => d.type + i)
              .transition()
              .ease(d3.easeLinear)
              .duration(0)
              //.delay((d, i) => i * 10)
              .delay(0)
              .attr("transform", getPlaneTransform)
              .attr("width", function(d){return size[d.type]})
              .attr("height", function(d){return size[d.type]})
              .attr("xlink:href", function(d){return color[d.type]});//(d) => getplanicon(d.type));

    images.on("mouseover", (e, d) => {
                d3.select("div #altitude").html(`<span>${d.alt}</span>`);
                d3.select("div #info").html(`<span>${d.Callsign}</span>`);
              });

    exit.remove();
}

var getplanicon = function(sign){
  //s = sign.slice(0,3);
  console.log(sign);
  if (sign === '0.0'){
    return "./pic/Cargo.png";

  }
  else{
    return "./pic/Cargo3.png";
  }
}

function step() {
  update(x.invert(currentValue));
  currentValue = currentValue + (targetValue/(endDate - startDate))*1000;
  if (currentValue > targetValue) {
    moving = false;
    currentValue = 0;
    clearInterval(timer);
    // timer = 0;
    playButton.text("Play");
    console.log("Slider moving: " + moving);
  }
}

function drawPlot(data) {
  var locations = plot.selectAll(".location")
    .data(data);
    //.filter(function(d) {return d.date < h});

  // if filtered dataset has more circles than already existing, transition new ones in
  locations.enter()
    .append("circle")
    .attr("class", "location")
    .attr("cx", function(d) { return x(d.date); })
    .attr("cy", height/2)
    .style("fill", function(d) { return d3.hsl(d.date/1000000000, 0.8, 0.8)})
    .style("stroke", function(d) { return d3.hsl(d.date/1000000000, 0.7, 0.7)})
    .style("opacity", 0.5)
    .attr("r", 8)
      .transition()
      .duration(400)
      .attr("r", 25)
        .transition()
        .attr("r", 8);

  // if filtered dataset has less circles than already existing, remove excess
  locations.exit()
    .remove();
}

var handleKeyEvent = function(e){

        // hitting u on the keyboard triggers flight data fetching and display
        //...?lamin=41.31&lomin=-4.93&lamax=51.16&lomax=7.72

        d3.json("https://opensky-network.org/api/states/all").then((data) => {
          const geoData = (data) => {
            return data.states
              .map((d) => {
                return {
                  id: d[0],
                  callsigh: d[1],
                  lon: d[5],
                  lat: d[6],
                  bearing: d[10],
                  alt: d[13],
                  on_ground: d[8],
                  bora_altitude: d[7],
                };
              })
              .filter((d) => d.lon != null && d.callsigh != "");
          };

          //add Plain
          ctx.currentFlights = geoData(data);
          const planes = d3.select("g #planes").selectAll("image").data(ctx.currentFlights);

          const images = planes.enter().append("image");

          images.attr("class", (d, i) => d.callsigh + i)
            .transition()
            .duration(100)
            .delay((d, i) => i * 10)
            .attr("transform", getPlaneTransform)
            .attr("width", 8)
            .attr("height", 8)
            .attr("xlink:href", (d) => getplanicon(d.callsigh));
          images.on("mouseover", (e, d) => {
            d3.select("div #altitude").html(`<span>${d.alt}</span>`);
            d3.select("div #info").html(`<span>${d.callsigh}</span>`);
          });
          planes.exit().remove();
      });

};



function drawflight(e){
  //console.log(data);


  //d3.json("./data/100_2016.json").then((data) => {
  $.getJSON("./data/100_2016.json", function (data){

      const datas = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);

      //console.log(datas);
      ctx.currentFlights = datas;

      d3.selectAll("image").remove();

      const planes = d3.select("g #planes").selectAll("image").data(datas);

      var exit = planes.exit();

      const images = planes.enter().append("image");

      images.attr("class", (d, i) => d.Callsign + i)
                .transition()
                .ease(d3.easeLinear)
                .duration(0)
                //.delay((d, i) => i * 10)
                .delay(0)
                .attr("transform", getPlaneTransform)
                .attr("width", 4)
                .attr("height", 4)
                .attr("xlink:href", (d) => getplanicon(d.cls));

      images.on("mouseover", (e, d) => {
                  d3.select("div #altitude").html(`<span>${d.alt}</span>`);
                  d3.select("div #info").html(`<span>${d.Callsign}</span>`);
                });

      exit.remove();

    });
};

var getPlaneTransform = function(d){
    var xy = PROJECTIONS.ER([d.lon, d.lat]);
    var sc = 4*ctx.scale;
    var x = xy[0] - sc;
    var y = xy[1] - sc;
    //if (d.bearing != null && d.bearing != 0){
    //    var t = `translate(${x},${y}) rotate(${d.bearing} ${sc} ${sc})`;
    //    return (ctx.scale == 1) ? t : t + ` scale(${ctx.scale})`;
    //}
    //else {
        var t = `translate(${x},${y})`;
        return (ctx.scale == 1) ? t : t + ` scale(${ctx.scale})`;
    //}
};

var getplanicon = function(sign){
  //s = sign.slice(0,3);
  console.log(sign);
  if (sign === '1'){
    return "./pic/Cargo.png";
  }
  else{
    return "./pic/Passenger.png";
  }
}


function update(h) {
  // update position and text of label according to slider scale
  handle.attr("cx", x(h));
  label
    .attr("x", x(h))
    .text(formatTimestamp(Math.floor(h/1000)*1000));

  ctx.timestamp = Math.floor(h/1000)*1000;

  //d3.json("./data/1000_2016.json").then((data) => {
  $.getJSON("./data/1000_2016.json", function (data){
      ctx.data1 = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
  });
  //GetVal();
  //console.log(document.getElementById("year").value === "2016")

  //if(document.getElementById("year").value === "2016"){
  //    d3.json("./data/vis_1000_2016.json").then((data) => {
  //        ctx.data1 = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
  //      });
  //}
  //else if (document.getElementById("year").value === "2017") {
  //  d3.json("./data/vis_1000_2017.json").then((data) => {
  //      ctx.data1 = data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
  //    });
  //};
  //ctx.data1 = datas;
  d3.selectAll("image").remove();

  //drawPlot(newData);

  //newData = dataset;
  drawflight2(ctx.data1);
  drawcargo();
  drawpassenger();
  //altitudeBar();
  //drawflight();
}


//vega

function drawpassenger(){
    var vegaBar = {
      "width": 400,
      "height": 100,
      "padding": 5,

    "data": {
        "values": ctx.data1.filter((b) => b.type == 0.0)
      },
    "mark": "bar",
    "encoding": {
      "x": {"title": "Passenger Airlines Name", "field": "Callsign", "type": "nominal", "axis": {"labelAngle": 0}, "sort": {"op": "count", "field": "Icao24"}},
      "y": {"title": "Count", "aggregate": "count",  "type": "quantitative"},
      "color": {
        "field": "Count",
        "title": "Passenger",
        "scale": {"range": ["#6c4e97"]}
      }
    },
    "background": 'transparent',

  };
  vegaEmbed("#vegachart", vegaBar);

}

function drawcargo(){

  var demodata = [
      {"a": "A", "b": 28}, {"a": "B", "b": 55}, {"a": "C", "b": 43},
      {"a": "D", "b": 91}, {"a": "A", "b": 30}, {"a": "B", "b": 25},      {"a": "C", "b": 3},
      {"a": "D", "b": 9}
    ];

  //console.log(ctx.data1);

  var vegaBar = {
    "width": 400,
    "height": 100,
    "padding": 5,

  "data": {
      "values": ctx.data1.filter((b) => b.type == 1.0)
    },
  "mark": "bar",
  "encoding": {
    "x": {"title": "Cargo Airlines Name","field": "Callsign", "type": "nominal", "axis": {"labelAngle": 0}, "sort": {"op": "count", "field": "Icao24"}},
    "y": {"title": "Count", "aggregate": "count",  "type": "quantitative"},
    "color": {
      "field": "Count",
      "title": "Cargo",
      "scale": {"range": ["#d5855a"]}
    }
  },
  "transform": [
    {"fold": ["Cargo"]}
  ],
  "background": 'transparent',

};
  vegaEmbed("#cargo", vegaBar);

}
