import * as d3 from 'd3';
import { Subscriber } from 'rxjs';
import { isNotNull, isNull } from './utils';
import { Position } from '../models/svg.models';
import { svg } from './svg.rendering.service';

export interface GroupInput {
    node: any,
    width?: number,
    height?: number,
    fontRatio?: number,
    styleClass?: string
}

// *****************************************************************************
// Components
// *****************************************************************************
export interface ToolTipBuilder {
    node: any,
    svg: any,
    enableText?: boolean,
    defaultText?: string,
    initializer?: Function,
    styleClass?: string,
    fontSize?: number,
    paddingX?: number,
    paddingY?: number
}
export class ToolTip {
    private group: any;
    private toolTipsLabel: any;
    private content: any;
    private mainContent:any;

    private paddingX: number;
    private paddingY: number;
    private background: any;
    private mouseEvent: Subscriber;
    private fontSize: number;

    constructor(private config: ToolTipBuilder) {
        this.init();
    }

    private init() {
        this.paddingX = isNull(this.config.paddingX) ? 1.6 : this.config.paddingX;
        this.paddingY = isNull(this.config.paddingY) ? 1.2 : this.config.paddingY;

        this.fontSize = isNull(this.config.fontSize) ? 10 : this.config.fontSize;
        this.group = this.config.node.append('g');
        this.group.attr('class', ['tool-tip', isNotNull(this.config.styleClass) ? this.config.styleClass : ''].join(' '));
        this.mainContent = this.group.append('g').attr('class', 'main-content');
        this.background = svg.builder.rect(this.mainContent, 1, 1, this.fontSize * 0.5, 'background');
        this.content = this.mainContent.append('g').attr('class', 'content');

        if (isNull(this.config.enableText) || this.config.enableText) {
            this.toolTipsLabel = svg.builder.text(
                this.content,
                isNull(this.config.defaultText) ? '' : this.config.defaultText,
                "tool-tip-label",
                this.fontSize);
        }
    }

    public toggleStyleClass(styleClass: string) {
        svg.builder.toggleOnffStyleClass(this.group, styleClass);
    }

    public updateText(label: string) {
        if (isNotNull(this.toolTipsLabel)) {
            this.toolTipsLabel.text(label);
        }

        let contentSize = svg.math.size(this.content, null, true);
        this.background.attr('width', contentSize.width * this.paddingX);
        this.background.attr('height', contentSize.height * this.paddingY);
        svg.transform.clean(this.content);
        svg.transform.center(this.content,this.background, true,true);
    }

    public trackMouse() {
        this.mouseEvent = svg.event.onMouseMove.subscribe({
            next: (mousePos) => {
                let config = this.config;
                let nodeSize = svg.math.size(config.node, null, true);
                let svgPos = svg.math.sizeHtmlElement(this.config.svg, null, true);
                let mainContentSize = svg.math.size(this.mainContent, null, true);
                let posX = mousePos.x - svgPos.x + 10;
                let posY = mousePos.y - svgPos.y + 10;

                if(posX> (nodeSize.width-mainContentSize.width)){
                    posX = nodeSize.width-mainContentSize.width;
                }

                if(posY > (nodeSize.height-mainContentSize.height)){
                    posY = nodeSize.height-mainContentSize.height;
                }
                svg.transform.translate(this.group, posX, posY);
            }
        });
    }

    public untrackMouse() {
        svg.transform.translate(this.group, -500, -500);
        if (isNotNull(this.mouseEvent)) {
            this.mouseEvent.unsubscribe();
        }
    }
}

// *****************************************************************************
// SVG BUILDER
// *****************************************************************************
export const builder = {

    // *****************************************************************************
    // CONTAINERS
    // *****************************************************************************

    svgContainer: (node, width: number, height: number, styleClass?: string) => {
        var result = d3.select(node)
            .append('svg')
            .attr('width', width)
            .attr('height', height);

        if (isNotNull(styleClass)) {
            result.attr('class', styleClass);
        }
        return result;
    },

    group: (input: GroupInput) => {
        let result = null;

        if (isNotNull(input.node)) {
            let currentStyleClass = ['group', isNull(input.styleClass) ? '' : input.styleClass];
            result = input.node.append('g')
                .attr("class", currentStyleClass.join(' '));

            if (isNotNull(input.width)) {
                result.attr("width", input.width);
            }
            if (isNotNull(input.height)) {
                result.attr("height", input.height);
            }
            if (isNotNull(input.fontRatio)) {
                result.attr("fontRatio", input.fontRatio);
            }
        }

        return result;
    },

    // *****************************************************************************
    // SHAPES
    // *****************************************************************************

    path: (points: Position[], closePath: boolean) => {
        var result = ["M"];

        var pointsrender = []
        if (isNotNull(points)) {
            for (let vertix of points) {
                let cmd = isNull(vertix.cmd) ? '' : vertix.cmd + ' ';
                result.push(`${cmd}${vertix.x},${vertix.y}`)
            }
        }

        if (closePath) {
            result.push("z");
        }

        return result.join(" ");
    },

    pathNode: (node: any, points: Position[], closePath?: boolean, styleclass?: string) => {
        return node.append('path')
            .attr("d", svg.builder.path(points, isNull(closePath) ? false : closePath))
            .attr("class", ['path', isNull(styleclass) ? '' : styleclass].join(' '));
    },

    rect: (layer: any, width: number, height: number, round: number, styleClass: string, absolut?: boolean) => {
        let result = null;
        if (isNotNull(layer)) {
            let currentStyleClass = ['rect', isNull(styleClass) ? '' : styleClass];
            let dimention = {
                "width": width,
                "height": height
            };

            if (isNotNull(absolut) && !absolut) {
                dimention = svg.math.computeDimension(layer, width, height, 0.10);
            }

            let rx = isNull(round) ? 0 : dimention.height * round;
            result = layer.append('rect')
                .attr("width", dimention.width)
                .attr("height", dimention.height)
                .attr("ry", rx)
                .attr("rx", rx)
                .attr("class", currentStyleClass.join(' '))
                .attr("x", 0)
                .attr("y", 0);
        }

        return result;
    },

    circle: (layer: any, radius: number, styleClass: string) => {
        let result = null;
        if (isNotNull(layer)) {
            let currentStyleClass = ['circle', isNull(styleClass) ? '' : styleClass];
            let dimention = svg.math.computeDimension(layer, radius, radius, 0.10);
            result = layer.append('circle')
                .attr("r", dimention.width)
                .attr("class", currentStyleClass.join(' '))
                .attr("cx", 0)
                .attr("cy", 0);
        }

        return result;
    },

    ellipse: (layer: any, width: number, height: number, styleClass: string) => {
        let result = null;
        if (isNotNull(layer)) {
            let currentStyleClass = ['ellipse', isNull(styleClass) ? '' : styleClass];
            let dimention = svg.math.computeDimension(layer, width, height, 0.10);
            result = layer.append('ellipse')
                .attr("rx", dimention.width)
                .attr("ry", dimention.height)
                .attr("class", currentStyleClass.join(' '))
                .attr("cx", 0)
                .attr("cy", 0);
        }

        return result;
    },

    text: (layer: any, label: string, styleClass: string, fontSize?: number) => {
        let result = null;
        let layerSize = svg.math.size(layer, null, true);
        let fontRatio = layerSize.fontratio;



        if (isNotNull(layer)) {
            let currentFontSize = isNull(fontSize) || fontRatio <= 0 ? 12 : fontSize;
            if (isNotNull(fontRatio)) {
                currentFontSize = currentFontSize * fontRatio;
            }
            let currentStyleClass = ['text', isNull(styleClass) ? '' : styleClass];
            result = layer.append('svg:text')
                .attr("class", currentStyleClass.join(' '))
                .attr("x", 0)
                .attr("y", 0)
                .attr("style", `font-size:${currentFontSize}px`);

            result.text(label)
        }

        return result;
    },


    toolTip: (builder: ToolTipBuilder): ToolTip => new ToolTip(builder),

    // *****************************************************************************
    // STYLE AND COLORS
    // *****************************************************************************
    toggleOnStyleClass: (node: any, styleclass: string) => {
        let currentStyleClass = node.attr("class");
        if (isNull(currentStyleClass)) {
            currentStyleClass = '';
        }
        if (currentStyleClass.indexOf(styleclass) == -1) {
            node.attr("class", currentStyleClass + ' ' + styleclass);
        }
    },
    toggleOnffStyleClass: (node: any, styleclass: string) => {
        let currentStyleClass = node.attr("class");
        if (isNull(currentStyleClass)) {
            currentStyleClass = '';
        }
        if (currentStyleClass.indexOf(styleclass) != -1) {
            node.attr("class", currentStyleClass.replace(styleclass, ''));
        }
    },
    toggleStyleClass: (node: any, styleclass: string) => {
        let currentStyleClass = node.attr("class");
        if (isNull(currentStyleClass)) {
            currentStyleClass = '';
        }

        if (currentStyleClass.indexOf(styleclass) == -1) {
            node.attr("class", currentStyleClass + ' ' + styleclass);
        } else {
            node.attr("class", currentStyleClass.replace(styleclass, ''));
        }
    },

    color: (currentValue: number, minValue: number, maxValue: number, from: RgbColor, to: RgbColor) => {
        let cleanFrom = builder._cleanRgb(from);
        let cleanTo = builder._cleanRgb(to);

        let red = svg.math.intervalProjection(currentValue, cleanFrom.r, cleanTo.r, minValue, maxValue);
        let green = svg.math.intervalProjection(currentValue, cleanFrom.g, cleanTo.g, minValue, maxValue);
        let blue = svg.math.intervalProjection(currentValue, cleanFrom.b, cleanTo.b, minValue, maxValue);

        return `rgb(${red},${green},${blue})`;
    },
    _cleanRgb: (color: RgbColor) => {
        let red = 0;
        let green = 0;
        let blue = 0;

        if (isNotNull(color)) {
            red = isNull(color.r) ? 0 : builder._normalizeColor(color.r);
            green = isNull(color.g) ? 0 : color.g;
            blue = isNull(color.b) ? 0 : color.b;
        }

        return {
            r: red,
            g: green,
            b: blue
        }
    },
    _normalizeColor: (value) => {
        let result = value;
        if (result < 0) {
            result = 0;
        }
        if (result > 255) {
            result = 255;
        }
        return result;
    },

    linearGradient: (node: any, name: string) => {
        var localName = name.split('-').join('_');
        var result = node.append("linearGradient")
            .attr("id", localName)
            .attr("x1", "0%")
            .attr("y1", "0%")
            .attr("x2", "0%")
            .attr("y2", "100%");

        result.append("stop")
            .attr("offset", "0%")
            .attr("class", localName + "_begin");

        result.append("stop")
            .attr("offset", "100%")
            .attr("class", localName + "_end");

        return result;
    },
};