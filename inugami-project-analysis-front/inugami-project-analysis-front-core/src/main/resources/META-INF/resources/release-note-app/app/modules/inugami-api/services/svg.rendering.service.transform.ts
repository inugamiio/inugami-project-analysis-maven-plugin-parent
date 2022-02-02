import * as d3 from 'd3';
import { isNotNull, isNull, asserts, isFalse } from './utils';
import { Position, Dimension, Size, TransformationInfo } from '../models/svg.models';

import { svg } from './svg.rendering.service';

export const DEFAULT_FONT_SIZE = 12;
export const TWO_PI = 2 * Math.PI;
export const TWO_PI_RATIO = (2 * Math.PI) / 360;

export const transform = {

    // *****************************************************************************
    // SCALES
    // *****************************************************************************
    scale: (node, scaleX: number, scaleY: number) => {
        if (isNotNull(node)) {
            var positions = svg.transform.extractTranformInformation(node);
            positions.scaleX = isNull(scaleX) ? 0 : scaleX;
            positions.scaleY = isNull(scaleY) ? 0 : scaleY;

            svg.transform._genericTransform(node, positions);
        }
    },
    scaleX: (node, scaleX: number) => {
        if (isNotNull(node)) {
            var positions = svg.transform.extractTranformInformation(node);
            positions.scaleX = isNull(scaleX) ? 0 : scaleX;

            svg.transform._genericTransform(node, positions);
        }
    },

    scaleY: (node, scaleY: number) => {
        if (isNotNull(node)) {
            var positions = svg.transform.extractTranformInformation(node);
            positions.scaleY = isNull(scaleY) ? 0 : scaleY;

            svg.transform._genericTransform(node, positions);
        }
    },
    matrix: (node, scaleX: number, scaleY: number, posX: number, posY: number) => {
        if (isNotNull(node)) {
            var data = [isNull(scaleX) ? 0 : scaleX,
                0,
                0,
            isNull(scaleY) ? 0 : scaleY,
            isNull(posX) ? 0 : posX,
            isNull(posY) ? 0 : posY];
            node.attr("transform", "matrix(" + data.join(',') + ")");
        }
    },

    // *****************************************************************************
    // TRANSLATES
    // *****************************************************************************
    translateX: (node, posX: number) => {
        if (isNotNull(node)) {
            var positions = svg.transform.extractTranformInformation(node);
            positions.x = isNull(posX) ? 0 : posX;

            svg.transform._genericTransform(node, positions);
        }
    },

    translateY: (node, posY: number) => {
        if (isNotNull(node)) {
            var positions = svg.transform.extractTranformInformation(node);
            positions.y = isNull(posY) ? 0 : posY;

            svg.transform._genericTransform(node, positions);
        }
    },

    translate: (node, posX: number, posY: number) => {
        if (isNotNull(node)) {
            var positions = svg.transform.extractTranformInformation(node);
            positions.x = isNull(posX) ? 0 : posX;
            positions.y = isNull(posY) ? 0 : posY;

            svg.transform._genericTransform(node, positions);
        }
    },


    // *****************************************************************************
    // TOOLS
    // *****************************************************************************
    clean: (node) => {
        if(isNotNull(node.attr('transform'))){
            node.attr('transform', null);
        }
    },

    extractTranformInformation: (node): TransformationInfo => {
        asserts.notNull(node);
        var result = {
            x: null,
            y: null,
            scaleX: null,
            scaleY: null
        };


        var attrTransfo = node.attr("transform");

        if (isNotNull(attrTransfo)) {

            var regex = new RegExp('(?:([^(]+)[(])([^)]+)(?:[)])');
            var group = regex.exec(attrTransfo);

            switch (group[1]) {
                case "translate":
                    var data = group[2].split(',');
                    result.x = Number(data[0]);
                    result.y = Number(data[1]);
                    break;

                case "scale":
                    var data = group[2].split(',');
                    result.scaleX = Number(data[0]);
                    result.scaleY = Number(data[1]);
                    break;

                case "matrix":
                    var data = group[2].split(',');
                    result.scaleX = Number(data[0]);
                    result.scaleY = Number(data[3]);
                    result.x = Number(data[4]);
                    result.y = Number(data[5]);
                    break;
            }
        }


        return result;
    },


    // *****************************************************************************
    // ALIGN
    // *****************************************************************************
    scaleByNode: (compo: any, svgContainer: any, onX: boolean, onY: boolean) => {
        let containerSize = svg.math.sizeHtmlElement(transform._getHtmlNode(svgContainer), null, true);
        let pos = svg.math.sizeHtmlElement(transform._getHtmlNode(compo), transform._getHtmlNode(svgContainer), true);
        if (onX) {
            let diffSizeX = containerSize.width / pos.width;
            svg.transform.scaleX(compo, diffSizeX);
        }
        if (onY) {
            let diffSizeY = containerSize.height / pos.height;
            svg.transform.scaleY(compo, diffSizeY);
        }
    },
    scaleSameRatio: (compo: any, svgContainer: any) => {
        let containerSize = svg.math.sizeHtmlElement(transform._getHtmlNode(svgContainer), null, true);
        let pos = svg.math.sizeHtmlElement(transform._getHtmlNode(compo), transform._getHtmlNode(svgContainer), true);

        let diffSizeX = containerSize.width / pos.width;
        let diffSizeY = containerSize.height / pos.height;
        let delta = diffSizeY < diffSizeX ? diffSizeY : diffSizeX;

        svg.transform.scaleX(compo, delta);
        svg.transform.scaleY(compo, delta);

    },
    center: (compo: any, svgContainer: any, onX: boolean, onY: boolean) => {

        let containerSize = svg.math.sizeHtmlElement(transform._getHtmlNode(svgContainer), null, true);
        let pos = svg.math.sizeHtmlElement(transform._getHtmlNode(compo), transform._getHtmlNode(svgContainer), true);

        if (onX) {
            let diffSizeX = containerSize.width - pos.width;
            let newPosX = (-pos.x)+(diffSizeX / 2);
            svg.transform.translateX(compo, newPosX);
        }
        if (onY) {
            let diffSizeY = containerSize.height - pos.height;
            let newPosY = (-pos.y) + (diffSizeY / 2);
            svg.transform.translateY(compo, newPosY);
        }
    },

    _getHtmlNode : (element:any) =>{
        return isNull(element.node) ? element : element.node();
    },
    // *****************************************************************************
    // PRIVATES
    // *****************************************************************************
    _noScale: function (data) {
        asserts.notNull(data);
        return isNull(data.scaleX) && isNull(data.scaleY);
    },
    _noTranslate: function (data) {
        asserts.notNull(data);
        return isNull(data.x) && isNull(data.y);
    },

    _genericTransform: function (node, transfo) {
        asserts.notNull(node);
        asserts.notNull(transfo);

        if (svg.transform._noScale(transfo)) {
            node.attr("transform", "translate(" + [isNull(transfo.x) ? 0 : transfo.x,
            isNull(transfo.y) ? 0 : transfo.y]
                .join(',') + ")");
        }
        else if (svg.transform._noTranslate(transfo)) {
            node.attr("transform", "scale(" + [isNull(transfo.scaleX) ? 0 : transfo.scaleX,
            isNull(transfo.scaleY) ? 0 : transfo.scaleY]
                .join(',') + ")");
        }
        else {
            svg.transform.matrix(node,
                isNull(transfo.scaleX) ? 0 : transfo.scaleX,
                isNull(transfo.scaleY) ? 0 : transfo.scaleY,
                isNull(transfo.x) ? 0 : transfo.x,
                isNull(transfo.y) ? 0 : transfo.y);
        }
    }
};