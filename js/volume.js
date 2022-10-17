//let width2 = window.outerWidth, height2 = window.outerHeight
let width2 = 960, height2 = 500;
var htimestamp;


var div = d3.select("#hotmap").append("div")
    .attr("class", "tooltip")
    .style("opacity", 0);


let hotsvg = d3.select("#hotmap")
              .append("svg")
              .attr("width", width2)
              .attr("height", height2)


let group1 = hotsvg.append('g')
let group2 = hotsvg.append('g')

function formatTimestamp(timestamp) {
    //if (sliderDateFormat === undefined) {
    sliderDateFormat = new Date();
    //}
    sliderDateFormat.setTime(timestamp * 1000);
    return sliderDateFormat.toUTCString();
}

let europeProjection = d3.geoMercator()
  	.center([ 7, 56])
    .scale([ width2  / 1])
    .translate([ width2 / 2, height2 / 2 ])


    var formatDateIntoYear = d3.timeFormat("%Y");
    var formatDate = d3.timeFormat("%b %Y");
    var parseDate = d3.utcParse("%m/%d/%Y")

    //var startDate = new Date("2004-11-01"),
    //      endDate = new Date("2017-04-01");

    var startDate = 1474157029,
        endDate =   1474761429;

    var margin = {top:0, right:150, bottom:50, left:30},
        width = 500 -margin.left - margin.right,
        height = 200 - margin.top - margin.bottom;


    var x2 = d3.scaleLinear()
            .domain([startDate, endDate])
            .range([0, width])
            .clamp(true);

    var slider2 = hotsvg.append("g")
          .attr("class", "slider")
          .attr("transform", "translate(" + margin.left + "," + height / 2 + ")");

    slider2.append("line")
              .attr("class", "track2")
              .attr("x1", x2.range()[0])
              .attr("x2", x2.range()[1])
            .select(function() { return this.parentNode.appendChild(this.cloneNode(true)); })
              .attr("class", "track-inset2")
            .select(function() { return this.parentNode.appendChild(this.cloneNode(true)); })
              .attr("class", "track-overlay2")
              .call(d3.drag()
                  .on("start.interrupt", function() { slider.interrupt(); })
                  .on("start drag", function() { hue(x2.invert(event.x -35)); }));

    slider2.insert("g", ".track-overlay2")
              .attr("class", "ticks2")
              .attr("transform", "translate(0," + 18 + ")")
            .selectAll("text")
              .data(x2.ticks(10))
              .enter()
              .append("text")
              .attr("x", x2)
              .attr("y", 10)
              .attr("text-anchor", "middle")
              //.text(function(d) { return formatTimestamp(d); });

    var label2 = slider2.append("text")
              .attr("class", "label")
              .attr("text-anchor", "middle")
              .text(formatTimestamp(startDate))
              .attr("transform", "translate(0," + (-25) + ")")

    var handle2 = slider2.insert("circle", ".track-overlay2")
              .attr("class", "handle")
              .attr("r", 9);

// The path generator uses the projection to convert the GeoJSON
// geometry to a set of coordinates that D3 can understand

pathGenerator = d3.geoPath().projection(europeProjection)
geoJsonUrl = "https://gist.githubusercontent.com/spiker830/3eab0cb407031bf9f2286f98b9d0558a/raw/7edae936285e77be675366550e20f9166bed0ed5/europe_features.json"


var color = ["#6c4e97","#ED8739"];

// Request the GeoJSON
d3.json(geoJsonUrl).then(geojson => {
  // Tell D3 to render a path for each GeoJSON feature
  group2.selectAll("path")
    .data(geojson.features)
    .enter()
    .append("path")
    .attr("d", pathGenerator) // This is where the magic happens
    .attr("stroke", "white") // Color of the lines themselves
    .attr("fill", "#CCCCCC") // Color uses to fill in the lines
})


// Three function that change the tooltip when user hover / move / leave a cell
var size = d3.scaleLinear()
      .domain([1,116])  // What's in the data
      .range([ 4, 8])

var size1 = d3.scaleLinear()
      .domain([1,116])  // What's in the data
      .range([ 4, 8])

function hue(h) {
  d3.selectAll("myCircles").remove();
  handle2.attr("cx", x2(h));
  label2
    .attr("x", x2(h))
    .text(formatTimestamp(h));
  htimestamp = h;




    d3.json("./data/2016_airportspoint.json").then((data) => {
        const Markers = (data).filter((d) => d.time < htimestamp);
          //.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
          //data.filter((d) => Math.floor(d.timeAtServer) == ctx.timestamp);
        //console.log(Markers);

        group2.selectAll("myCircles")
                .data(Markers)
                .enter()
                .append("circle")
                  .attr("cx", function(d){
                    if (d.type == 1) {
                      return europeProjection([d.lon, d.lat])[0]+5;
                    }
                    else{
                      return europeProjection([d.lon, d.lat])[0];
                    }

                  })
                  .attr("cy", function(d){
                    if (d.type == 1) {
                      return europeProjection([d.lon, d.lat])[1]+5;
                    }
                    else{
                      return europeProjection([d.lon, d.lat])[1];
                    }
                  })
                  .attr("r",  function(d){
                    if (d.type === 1) {
                        return size1(Markers.filter((i) => i.type === 1 && i.name === d.name).length );
                      }
                    else{
                        return size(Markers.filter((i) => i.type === 0 && i.name === d.name).length );
                    }
                  })
                  .attr("class", "circle")
                  .style("fill", function(d){return color[d.type]})
                  .attr("stroke", function(d){return color[d.type]})
                  .attr("stroke-width", .1)
                  .attr("fill-opacity", .3)
                .on("mouseover", function(event,d) {

                   d.count1 =  Markers.filter((i) => i.type === d.type && i.name === d.name).length;
                   //d.count0 =  Markers.filter((i) => i.type === 0 && i.name === d.name).length;
                   if (d.type === 1) {d.airtype = "Cargo"}
                   else{d.airtype = "Passenger"};

                   div.transition()
                     .duration(200)
                     .style("opacity", .9);
                   div.html(`<span>${d.name}</span>` + "<br/>" + "Type: " + d.airtype + "<br/>" + "Count: " + d.count1)
                     .style("left", (event.pageX) + "px")
                     .style("top", (event.pageY - 28) + "px");
                   })
                 .on("mouseout", function(d) {
                   div.transition()
                     .duration(500)
                     .style("opacity", 0);
                   });

                //.on("mouseover",  (e, d) => {
                //  d3.select("div #airportname").html(`<span>${d.name}</span>`);
                //  d3.select("div #infos").html(`<span>${d.lon}</span>`);
                //          })

      });

}



`function altitudeBar(){
  var altdata = ctx.data1.forEach((item, i) => {
    ctx.data1[i]["alt"] = Math.floor(item.alt/5000)*5000;
  });

  console.log(altdata);
  var vegaBar = {

    "data": { "values": altdata},
    "transform": [
      {"calculate": "datum.type == 1 ? 'Cargo' : 'Passenger'", "as": "Type"}
      ],
    "spacing": 0,
    "hconcat": [{
    "transform": [{
      "filter": {"field": "Type", "equal": "Cargo"}
    }],
    "title": "Cargo",
    "mark": "bar",
    "encoding": {
      "y": {
        "field": "alt", "axis": null, "sort": "descending"
      },
      "x": {
        "aggregate": "count", "field": "Icao24",
        "title": "Number of Cargo",
        "axis": {"format": "s"},
        "sort": "descending"
      },
      "color": {
        "field": "Type",
        "scale": {"range": ["#675193", "#ca8861"]},
        "legend": null
      }
    }
  }, {
    "width": 20,
    "view": {"stroke": null},
    "mark": {
      "type": "text",
      "align": "center"
    },
    "encoding": {
      "y": {"field": "alt", "type": "ordinal", "axis": null, "sort": "descending"},
      "text": {"field": "alt", "type": "quantitative"}
    }
  }, {
    "transform": [{
      "filter": {"field": "Type", "equal": "Passenger"}
    }],
    "title": "Passenger",
    "mark": "bar",
    "encoding": {
      "y": {
        "field": "alt", "title": null,
        "axis": null, "sort": "descending"
      },
      "x": {
        "aggregate": "count", "field": "alt",
        "title": "Number of Passenger",
        "axis": {"format": "s"}
      },
      "color": {
        "field": "Type",
        "legend": null
      }
    }
  }],
};

vegaEmbed("#altitudegroup", vegaBar);

};
`
