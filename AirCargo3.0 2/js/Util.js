var Util={};

/**
 * judge device type(pc or phone)
 * return string
 * blog link:http://www.cnblogs.com/babycool/p/3583114.html
 */
Util.getDeviceType = function() {
  var userAgent = navigator.userAgent.toLowerCase();
  var isIpad = userAgent.match(/ipad/i) == "ipad";
  var isIphoneOs = userAgent.match(/iphone os/i) == "iphone os";
  var isMidp = userAgent.match(/midp/i) == "midp";
  var isUc7 = userAgent.match(/rv:1.2.3.4/i) == "rv:1.2.3.4";
  var isUc = userAgent.match(/ucweb/i) == "ucweb";
  var isAndroid = userAgent.match(/android/i) == "android";
  var isCE = userAgent.match(/windows ce/i) == "windows ce";
  var isWM = userAgent.match(/windows mobile/i) == "windows mobile";
  if (isIpad || isIphoneOs || isMidp || isUc7 || isUc || isAndroid || isCE || isWM) {
    return "phone";
  } else {
    return "pc";
  }
};

/**
 * 用于事件的处理
 * ie 不支持参数默认赋值，link:https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Functions/Default_parameters
 */
Util.Event = {
  addHandler: function(element, type, handler, useCapturing) {
    if(element.addEventListener) {
      // IE9、Firefox、Safari、Chrome 和Opera 支持DOM2 级事件处理程序。
      element.addEventListener(type, handler, useCapturing || false);
    } else if(element.attachEvent) {
      // IE<9
      element.attachEvent('on' + type, handler);
    } else {
      element['on' + type] = handler;
    }
  },
  removeHandler: function(element, type, handler,useCapturing) {
    if(element.removeEventListener) {
      // IE9、Firefox、Safari、Chrome 和Opera 支持DOM2 级事件处理程序。
      element.removeEventListener(type, handler, useCapturing || false);
    } else if(element.detachEvent) {
      // IEe<9
      element.detachEvent('on' + type, handler);
    } else {
      element['on' + type] = null;
    }
  },
  getEvent: function(event) {
    return event ? event : window.event;
  },
  // 阻止事件传播
  stopPropagation: function(event) {
    if (event.stopPropagation){
      event.stopPropagation();
    } else {
      event.cancelBubble = true;
    }
  },
  // 阻止默认行为
  stopDefault: function(event) {
    if (event.preventDefault){
      event.preventDefault();
    } else {
        event.returnValue = false;
    }
  }
}

// 获取一个随机数字符串，用来做对比
Util.getRandomNum = function(len) {
  var numString = String(Math.random());
  if (len) {
    return numString.substring(0,len);
  } else {
    return numString;
  }
}

// 插入文本
Util.appendText = function(ele, text) {
  var newELe = document.createElement('p');
  newELe.innerText = (text || '')+Util.getRandomNum(5);
  ele && ele.appendChild(newELe);
}

// 加载中提示
Util.loading = {
  create: function() {
    var ele = document.createElement('div');
    var text = document.createTextNode('loading ~ ~ ~');
    ele.appendChild(text);
    this.loadingEle = ele;
  },
  show:function() {
    this.create.bind(this)();
    document.querySelector('body').appendChild(this.loadingEle);
  },
  hide: function() {
    document.querySelector('body').removeChild(this.loadingEle);
  }
}

// canvas 各种处理
Util.CANVAS = {
  // 处理显示模糊问题
  createElement: function(w=300,h=150) {
    var ratio = window.devicePixelRatio || 1;
    var canvas = document.createElement('canvas');
    canvas.width = w * ratio; // 实际渲染像素
    canvas.height = h * ratio; // 实际渲染像素
    canvas.style.width = `${w}px`; // 控制显示大小
    canvas.style.height = `${h}px`; // 控制显示大小
    canvas.getContext('2d').setTransform(ratio, 0, 0, ratio, 0, 0);
    return canvas;
  },
  // 根据提供的坐标点绘制直线
  drawLine: function({context,points,lineWidth=1,lineCap="butt",strokeStyle="#fff",fillStyle="#fff",isClose=false}){
    context.beginPath();
    const loopLen = points.length;
    // console.info('point',point)
    context.lineWidth = lineWidth;
    context.lineCap = lineCap;
    context.strokeStyle = strokeStyle;
    context.fillStyle = fillStyle;
    for (let index = 0; index < loopLen; index++) {
      const [x,y] = points[index];
      if (index === 0) {
        context.moveTo(x,y);
      } else {
        context.lineTo(x,y);
        if (isClose && index === loopLen-1) {
          const firstPoint = points[0];
          context.lineTo(firstPoint[0],firstPoint[1]);
        }
      }

    }
    context.stroke();
    if (isClose) {
      context.fill();
    }
    context.closePath();
  },
  // 绘制三角形
  drawTriangle: function({context,points,lineWidth=1,strokeStyle="#fff",fillStyle="#fff"}){
    context.beginPath();
    const loopLen = points.length;
    console.info('points',points)
    context.lineWidth = lineWidth;
    context.strokeStyle = strokeStyle;
    for (let index = 0; index < loopLen; index++) {
      const [x,y] = points[index];
      if (index === 0) {
        context.moveTo(x,y);
      } else {
        context.lineTo(x,y);
        if(index === loopLen-1) {
          const [x,y] = points[0]
          context.lineTo(x,y);
        }
      }

    }
    context.stroke();
    context.closePath();
  },
  // 绘制矩形
  drawRect: function({context,x, y, width, height,lineWidth=1,strokeStyle="#fff",fillStyle="#fff"}){
    context.beginPath();
    context.rect(x, y,width, height);
    context.fillStyle = fillStyle;
    context.fill();
    context.lineWidth = lineWidth;
    context.strokeStyle = strokeStyle;
    context.stroke();
    context.closePath();
  },
  // 绘制圆形
  drawArc: function({context, x, y, radius, startAngle, endAngle, anticlockwise=false,lineWidth=1,strokeStyle="#fff",fillStyle="#fff"}){
    context.beginPath();
    context.arc(x, y, radius, startAngle, endAngle, anticlockwise);
    context.fillStyle = fillStyle;
    context.fill();
    context.lineWidth = lineWidth;
    context.strokeStyle = strokeStyle;
    context.stroke();
    context.closePath();
  },
  // 生成有圆角的矩形
  drawRoundedRect: function(context, x, y, width, height, radius) {
    context.beginPath();
    context.arc(x + radius, y + radius, radius, Math.PI, Math.PI * 3 / 2);
    context.lineTo(width - radius + x, y);
    context.arc(width - radius + x, radius + y, radius, Math.PI * 3 / 2, Math.PI * 2);
    context.lineTo(width + x, height + y - radius);
    context.arc(width - radius + x, height - radius + y, radius, 0, Math.PI * 1 / 2);
    context.lineTo(radius + x, height + y);
    context.arc(radius + x, height - radius + y, radius, Math.PI * 1 / 2, Math.PI);
    context.closePath();
  },
  // 文本换行处理，并返回实际文字所占据的高度
  textEllipsis: function(context, text, x, y, maxWidth, lineHeight, row) {
    if (typeof text != 'string' || typeof x != 'number' || typeof y != 'number') {
      return;
    }
    var canvas = context.canvas;

    if (typeof maxWidth == 'undefined') {
      maxWidth = canvas && canvas.width || 300;
    }

    if (typeof lineHeight == 'undefined') {
      // 有些情况取值结果是字符串，比如 normal。所以要判断一下
      var getLineHeight = window.getComputedStyle(canvas).lineHeight;
      var reg=/^[0-9]+.?[0-9]*$/;
      lineHeight = reg.test(getLineHeight)? getLineHeight:20;
    }

    // 字符分隔为数组
    var arrText = text.split('');
    // 文字最终占据的高度，放置在文字下面的内容排版，可能会根据这个来确定位置
    var textHeight = 0;
    // 每行显示的文字
    var showText = '';
    // 控制行数
    var limitRow = row;
    var rowCount = 0;

    for (var n = 0; n < arrText.length; n++) {
      var singleText = arrText[n];
      var connectShowText = showText + singleText;
      // 没有传控制的行数，那就一直换行
      var isLimitRow = limitRow ? rowCount === (limitRow - 1) : false;
      var measureText = isLimitRow ? (connectShowText+'……') : connectShowText;
      var metrics = context.measureText(measureText);
      var textWidth = metrics.width;

      if (textWidth > maxWidth && n > 0 && rowCount !== limitRow) {
        var canvasShowText = isLimitRow?measureText:showText;
        context.fillText(canvasShowText, x, y);
        showText = singleText;
        y += lineHeight;
        textHeight += lineHeight;
        rowCount++;
        if (isLimitRow) {
          break;
        }
      } else {
        showText = connectShowText;
      }
    }
    if (rowCount !== limitRow) {
      context.fillText(showText, x, y);
    }

    var textHeightValue = rowCount < limitRow ? (textHeight + lineHeight): textHeight;
    return textHeightValue;
  },
  /**
   * 图像灰度处理
   * @param {*} context canvas 上下文
   * @param {*} sx 提取图像数据矩形区域的左上角 x 坐标。
   * @param {*} sy 提取图像数据矩形区域的左上角 y 坐标。
   * @param {*} sw 提取图像数据矩形区域的宽度。这要注意一下，canvas 标签上 width 属性值，不是渲染后实际宽度值，否则在高清手机屏幕下且做了高清处理，只能获取到部分图像宽度。
   * @param {*} sh 提取图像数据矩形区域的高度。这要注意一下，canvas 标签上 height 属性值，不是渲染后实际高度值，否则在高清手机屏幕下且做了高清处理，只能获取到部分图像高度。
   */
  toGray: function(context,sx, sy, sw, sh) {
    var imageData = context.getImageData(sx, sy, sw, sh);
    var colorDataArr = imageData.data;
    var colorDataArrLen = colorDataArr.length;
    for(var i = 0; i < colorDataArrLen; i+=4) {
      var gray=(colorDataArr[i]+colorDataArr[i+1]+colorDataArr[i+2])/3;
      colorDataArr[i] = gray;
      colorDataArr[i+1] = gray;
      colorDataArr[i+2] = gray;
    }
    context.putImageData(imageData,0,0);
  },
  /**
   * 获取透明所占百分比，初始参考透明值是 128
   * @param {object} context canvas 上下文对象
   * @param {number} opacity 透明度参考值
   */
  getOpacityPercentage: function(context, opacity = 128) {
    var imageData = context.getImageData(0,0,248,415);
    var colorDataArr = imageData.data;
    // console.info('color data:',colorDataArr);
    var colorDataArrLen = colorDataArr.length;
    var eraseArea = [];
    for(var i = 0; i < colorDataArrLen; i += 4) {
      // 严格上来说，判断像素点是否透明需要判断该像素点的a值是否等于0，
      if(colorDataArr[i + 3] < opacity) {
        eraseArea.push(colorDataArr[i + 3]);
      }
    }
    var divResult = eraseArea.length / (colorDataArrLen/4);
    var pointIndex = String(divResult).indexOf('.');
    if (pointIndex>-1) {
      divResult = String(divResult).slice(0,pointIndex+5);
    }
    return Number(divResult).toFixed(2);

  },
  /**
   * canvas.transform(sx, ry, rx, sy, tx, ty)
   * sx-0-水平缩放，ry-1-垂直倾斜，rx-2-水平倾斜，sy-3-垂直缩放，tx-4-水平移动，ty-5-垂直移动
   *
   * 为了方便使用 ，再次包装一下，参照 CSS 中的方法命名
   * @param {object} context canvas 上下文对象
   * @param {number} x 水平方向的移动
   * @param {number} y 垂直方向的移动
   */
  translate: function(context,x=0,y=0) {
    if (!context.transformData) {
      Util.CANVAS.resetTransform(context)
    }
    context.translate(x,y);
    let [sx,ry,rx,sy,tx,ty] = context.transformData;
    tx = sx * x + rx * y;
    ty = ry * x + sy * y;
    context.transformData = [sx,ry,rx,sy,tx,ty]
  },
  rotate: function(context,angle) {
    if (!context.transformData) {
      Util.CANVAS.resetTransform(context)
    }

    context.rotate(angle);
    let c = Math.cos(angle);
    let s = Math.sin(angle);
    let [sx,ry,rx,sy,tx,ty] = context.transformData;
    let newSX = sx * c + rx * s;
    let newRY = ry * c + sy * s;
    let newRX = sx * -s + rx * c;
    let newSY = ry * -s + sy * c;
    context.transformData = [newSX,newRY,newRX,newSY,tx,ty]
  },
  scale: function(context,x=1,y=1) {
    if (!context.transformData) {
      Util.CANVAS.resetTransform(context)
    }
    context.scale(x,y);
    let [sx,ry,rx,sy,tx,ty] = context.transformData;
    let newSX = sx * x;
    let newRY = ry * x;
    let newRX = rx * y;
    let newSY = sy * y;
    context.transformData = [newSX,newRY,newRX,newSY,tx,ty]
  },
  resetTransform: function(context) {
    var ratio = window.devicePixelRatio || 1;
    context.transformData = [1,0,0,1,0,0];
    context.setTransform(ratio,0,0,ratio,0,0);
  },
  // 获取点的坐标
  getPosition: function(context,px,py) {
    if (!context.transformData) {
      console.info('no data')
    }
    let x = px;
    let y = py;
    let [sx,ry,rx,sy,tx,ty] = context.transformData;
    px = x * sx + y*rx + tx;
    py = x * ry + y*sy + ty;
    return [px,py];
  },
  /**
   * 清除画布
   * @param {object} context canvas 上下文对象
   * @param {number} width 画布高度
   * @param {number} height 画布宽度
   */
  clear: function(context,width = 0,height = 0) {
    var centerX = width/2;
    var centerY = height/2;
    var maxRadius = Math.sqrt( Math.pow(centerX,2) + Math.pow(centerY,2) ) + 1;
    var radius = 10;
    context.beginPath();
    var count = setInterval(() => {
      if (radius>maxRadius) {
        clearInterval(count);
      }
      radius+=3;
      context.arc(centerX, centerY, radius, 0, Math.PI * 2);
      context.fill();
    }, 10);
  },
}

/**
 * 插入原文链接
 * @param {object} params
 */
Util.insertLink= function (params) {
  var linkType = params.type || 'segment';
  var eleClass = params.className || 'fix-header tc';
  var linkIndex = params.linkIndex;
  var title = params.title;
  var url = '';

  if (!linkIndex) {
    return;
  }
  switch (linkType) {
    case 'segment':
      url = 'https://github.com/XXHolic/segment/issues/' + linkIndex;
    break;
    case 'blog':
      url = 'https://github.com/XXHolic/blog/issues/' + linkIndex;
    break;

  }

  var insertEle = document.createElement('h2');
  insertEle.setAttribute('class',eleClass);
  var linkEle = document.createElement('a');
  linkEle.setAttribute('href',url);
  linkEle.setAttribute('target','_blank');
  var textNode = document.createTextNode('对应文：'+title);
  linkEle.appendChild(textNode);
  insertEle.appendChild(linkEle);
  var bodyEle = document.body;
  bodyEle.insertBefore(insertEle,bodyEle.firstElementChild);

}

/**
 * 获取焦点坐标
 * @param {object} event 事件对象
 * @param {object} element 焦点所在的元素
 * @param {object} buffer 偏移量，在手机上点击可能看不到，为了方便查看偏移一些距离
 */
Util.getPointCoordinate = function (event,element,buffer=0) {
  let isPc = Util.getDeviceType() === "pc";
  const point = isPc ? event:event.touches[0];
  const {offsetLeft,offsetTop} = element
  let xPos = parseInt(point.pageX - offsetLeft);
  let yPos = parseInt(point.pageY - offsetTop);
  // 手指移动时，为了在移动端方便查看，偏移了一些像素。
  if (!isPc) {
    xPos = xPos - buffer;
    yPos = yPos - buffer;
  }

  return {xPos,yPos}
}

/**
 * 获取随机数，包含最大值和最小值
 * @param {*} min
 * @param {*} max
 */
Util.getRandom = function(min, max) {
  let minValue = min;
  let maxValue = max;
  if (minValue > maxValue) {
    minValue = max;
    maxValue = min;
  }
  return Math.floor(Math.random() * (maxValue - minValue + 1) ) + minValue;
}

/**
 * 数学公式
 */
Util.Math = {
  tan: function (deg) {
    var rad = (deg * Math.PI) / 180;
    return Math.tan(rad);
  },
};

/**
 * n 是 start1 到 stop1 之间的值，映射到 start2 和 stop2 之间的值
 * 相当于缩放了 n 的值
 * @param {*} n
 * @param {*} start1
 * @param {*} stop1
 * @param {*} start2
 * @param {*} stop2
 * @returns
 */
Util.map = (n, start1, stop1, start2, stop2) => {
  return ((n - start1) / (stop1 - start1)) * (stop2 - start2) + start2;
};
