import * as d3 from 'd3';
import { isNotNull, isNull, asserts, isFalse } from './utils';
import { Position, Dimension, Size, TransformationInfo } from '../models/svg.models';
import { svg } from './svg.rendering.service';
export const DEFAULT_FONT_SIZE = 12;
export const TWO_PI = 2 * Math.PI;
export const TWO_PI_RATIO = (2 * Math.PI) / 360;

export const math = {
    convertToRadian: (angle: number) => {
        return angle * TWO_PI_RATIO;
    },

    /**
     * Allow to rotate a point by radian angle
     * @param x     : number : X position
     * @param y     : number : Y position
     * @param angle : number : angle in degre
     */
    rotate: (x, y, angle): Position => {
        var radian = svg.math.convertToRadian(angle);
        var cos = Math.cos(radian);
        var sin = Math.sin(radian);

        return {
            x: (x * cos - y * sin),
            y: (x * sin + y * cos)
        };
    },

    /**
     * Allow to rotate a point by radian angle
     * @param x     : number : X position
     * @param y     : number : Y position
     * @param angle : number : angle in degre
     */
    rotateByRef: (xRef, yRef, x, y, angle): Position => {
        var zX = x - xRef;
        var zY = y - yRef;
        var rotate = svg.math.rotate(zX, zY, angle);
        return {
            x: rotate.x + xRef,
            y: rotate.y + yRef
        }
    },


    /**
     * Allow to compute relational dimention
     * @param parent      : node        : component parent
     * @param heightRatio : number      : current component height ratio
     * @param widthRatio  : number      : current component width ratio
     * @param fontRatio   : number      : current font ratio
     */
    computeDimension: (parent, widthRatio, heightRatio, fontRatio): Dimension => {


        var resultHeightRatio = heightRatio;
        var resultWidthRatio = widthRatio;
        var resultFontRatio = fontRatio;

        if (isNotNull(parent)) {
            var parentSize = svg.math.size(parent,null,true);
            resultHeightRatio = 1;
            resultWidthRatio = 1;
            resultFontRatio = 1;
            var parentHeight = 1;
            var parentWidth = 1;
            var parentFont = 1;

            var localHeight = parentSize.height
            var localWidth = parentSize.width;
            var localFont = parentSize.fontratio;

            if (isNotNull(localHeight)) {
                parentHeight = parentHeight * localHeight;
            }
            if (isNotNull(localWidth)) {
                parentWidth = parentWidth * localWidth;
            }
            if (isNotNull(localFont)) {
                parentFont = parentFont * localFont;
            }


            if (isNotNull(heightRatio)) {
                resultHeightRatio = parentHeight * heightRatio;
            } else {
                resultHeightRatio = parentHeight;
            }

            if (isNotNull(widthRatio)) {
                resultWidthRatio = parentWidth * widthRatio;
            } else {
                resultWidthRatio = parentWidth;
            }

            if (isNotNull(fontRatio)) {
                resultFontRatio = parentFont * fontRatio;
            } else {
                resultFontRatio = parentFont;
            }
        }

        return {
            "height": resultHeightRatio,
            "width": resultWidthRatio,
            "font": resultFontRatio
        }
    },

    computeFontSize: (fontRatio, height, width, ratioByHeight, ratioByWidth): number => {
        var result = DEFAULT_FONT_SIZE;

        if (isNotNull(ratioByHeight) || isNotNull(ratioByWidth)) {
            if (ratioByHeight) {
                result = height * fontRatio;
            } else {
                result = width * fontRatio;
            }
        } else {
            var fullSize = height * width;
            result = fullSize * fontRatio;
        }
        return result;
    },

    size: function (component: any, svgCanva: any, absolut?: boolean): Size {
        var node = math._getNode(component);
        var info = node.getBoundingClientRect();

        var nodeHeight = isFalse(absolut) ? null : node.getAttribute("height");
        var nodeWidth = isFalse(absolut) ? null : node.getAttribute("width");
        var refX = 0;
        var refY = 0;

        if (isNotNull(svgCanva)) {
            var canvaPos = math._getNode(svgCanva).getBoundingClientRect();
            refX = canvaPos.x;
            refY = canvaPos.y;
        }
        return {
            "bottom": info.bottom,
            "width": isNull(nodeWidth) ? info.width : nodeWidth,
            "height": isNull(nodeHeight) ? info.height : nodeHeight,
            "left": info.left,
            "right": info.right,
            "top": info.top,
            "x": info.x - refX,
            "y": info.y - refY,
            "fontratio": isNull(node.getAttribute("fontratio")) ? 1 : node.getAttribute("fontratio")
        }
    },

    sizeHtmlElement: function (node: any, svgCanva: any, absolut?: boolean): Size {

        var info = node.getBoundingClientRect();

        var refX = 0;
        var refY = 0;

        if (isNotNull(svgCanva)) {
            var canvaPos = svgCanva.getBoundingClientRect();
            refX = canvaPos.x;
            refY = canvaPos.y;
        }

        var nodeHeight = absolut ? null : node.getAttribute("height");
        var nodeWidth = absolut ? null : node.getAttribute("width");

        return {
            "bottom": info.bottom,
            "width": isNull(nodeWidth) ? info.width : nodeWidth,
            "height": isNull(nodeHeight) ? info.height : nodeHeight,
            "left": info.left,
            "right": info.right,
            "top": info.top,
            "x": info.x - refX,
            "y": info.y - refY,
            "fontratio": isNull(node.getAttribute("fontratio")) ? 1 : node.getAttribute("fontratio")
        }
    },

    _getNode : (node)=>{
        if(isNotNull(node.node)){
            return node.node();
        }
        else{
            return node;
        }
    },

    positive: (value: number) => {
        return value < 0 ? value * -1 : value;
    },

    intervalProjection : (currentValue:number, refFrom:number, refTo:number, minValue:number, maxValue:number, decimal?:boolean)=>{
        let percent = (currentValue-minValue)/(maxValue-minValue);
        let refDelta = refTo-refFrom;

        let result = refFrom+(refDelta*percent);
        if(isNull(decimal) || !decimal){
            result = Math.round(result);
        }
        return result;
    }

};