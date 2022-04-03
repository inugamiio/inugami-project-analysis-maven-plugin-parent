
import { Subscriber } from 'rxjs';
import {
    Component,
    forwardRef,
    ViewChild,
    AfterViewInit,
    ViewContainerRef,
    ElementRef
} from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import * as d3 from 'd3';
import { svg } from '../../../inugami-api/services/svg.rendering.service';
import { Artifact, ProjectDependenciesGraph, ArtifactGraph, ArtifactGraphDependency } from '../../models/release.note';
import { Dependency, DependencyInfo } from '../../models/dependencies.check';
import { isNull, isNotNull, isNotEmpty } from '../../../inugami-api/services/utils';
import { ToolTip } from '../../../inugami-api/services/svg.rendering.service.builder';
import { TWO_PI } from '../../../inugami-api/services/svg.rendering.service.math';
import { Position } from '../../../inugami-api/models/svg.models';
import { mousePosition } from '../../../inugami-api/services/svg.rendering.service.event';

export const PROJECT_DEPENDENCY_COMPONENT_GRAPH_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ProjectDependencyGraphComponent),
    multi: true
};


const ON_HOVE_TOOL_TIPS = 'onHoverToolTips';
const ON_HOVE = 'onHover';
const TYPES = {
    mainArtifact: 'current-project',
    dependencyArtifact: 'dependencies',
    consumedArtifact: 'consumed-dependencies'
}


@Component({
    selector: 'project-dependency-graph',
    directives: [],
    providers: [PROJECT_DEPENDENCY_COMPONENT_GRAPH_VALUE_ACCESSOR],
    template: `
<div class="project-dependency-graph">
  <div class="svg-container" #container>

  </div>
  <div class="svg-container-end"></div>
</div>
<div class="project-dependency-graph-end"></div>
`
})
export class ProjectDependencyGraphComponent implements AfterViewInit {

    /**************************************************************************
    * ATTRIBUTES
    **************************************************************************/
    @ViewChild('container') content: ElementRef;
    private hasValue: boolean = false;
    private innerValue: ProjectDependenciesGraph;
    private mainArtifacts: any;
    private mainArtifactsList: any[] = [];

    private container: any;
    private width: number = 512;
    private height: number = 256;

    private rectHeight: number;
    private fontSize: number;
    private fontPaddingY = 1.4;
    private fontPaddingX = 1.2;
    private columnMargin = 0.2;
    private nodeMargin = 1.2;

    private columnLeft: number = 0;
    private columnMiddle: number = 0;
    private columnRight: number = 0;

    private svg: any;
    private mainLocator : any;
    private mainLocatorPosition : Position = {x:null, y:null};
    private svgContainer: any;
    private mainLayer: any;
    private nodesLayer: any;
    private mainNodesLayer: any;
    private dependenciesNodesLayer: any;
    private consumedDependenciesNodesLayer: any;

    private toolTipsCompo : ToolTip;
    private connectorsLayer: any;
    private artifactNodes: any = {};
    private mainArtifactNodes: any = [];
    private dependenciesArtifactNodes: any = [];
    private consumedArtifactNodes: any = [];

    private zoom : number = 1;
    private connectors: any = {};

    /**************************************************************************
    * INIT
    **************************************************************************/
    ngAfterContentInit() {

    }
    /**************************************************************************
    * API
    **************************************************************************/
    private cleanGraph() {

    }
    private initializeGraph() {
        if (isNotNull(this.content)) {
            let self = this;
            this.rectHeight = this.height / 10;
            this.fontSize = this.rectHeight * 0.8;

            this.container = this.content.nativeElement;
            this.svg = svg.builder.svgContainer(this.container, this.width, this.height, "svg");

            this.svg.on("wheel", (d)=>self.onZoom(d));
            this.svg.on("auxclick", (d)=>self.onMove(d));
            this.svg.on("dblclick", (d)=>self.onDbclick(d));


            this.svg.call(d3.drag()
            .on('drag',(e)=>this.onDrag(e))
          )

            this.svgContainer = this.svg.append("g").attr("class", "svg-container");
            let def = this.svg.append("defs");
            def.append("filter")
                .attr("style", "color-interpolation-filters:sRGB")
                .attr("id", "connectorBlur")
                .append("feGaussianBlur")
                .attr("stdDeviation", "0.0005")

            this.mainLocator = svg.builder.group({ node: this.svgContainer, styleClass: "locator" });
            this.mainLayer = svg.builder.group({ node: this.mainLocator, styleClass: "main-layer" });
            this.connectorsLayer = svg.builder.group({ node: this.mainLayer, styleClass: "connectors-layer" });
            this.nodesLayer = svg.builder.group({ node: this.mainLayer, styleClass: "nodes-layer" });


            this.dependenciesNodesLayer = svg.builder.group({ node: this.nodesLayer, styleClass: "dependencies" });
            this.consumedDependenciesNodesLayer = svg.builder.group({ node: this.nodesLayer, styleClass: "consumed-dependencies" });
            this.mainNodesLayer = svg.builder.group({ node: this.nodesLayer, styleClass: "main-artifact" });

            this.toolTipsCompo = svg.builder.toolTip({
                node: this.svg,
                svg: this.container,
                fontSize: 12
            });


            if (this.hasValue) {
                this.renderArtifacts();
                this.centerNodes();
                this.renderConnectors();
                this.centerMainLayer();
            }
        }
    }

    private renderArtifacts() {
        this.mainArtifacts = this.resolveMainArtifact();
        for (let artifact of this.innerValue.artifacts) {

            let artifactType = this.resolveArtifactType(artifact);
            this.renderArtifact(artifact, artifactType);
        }
    }



    private renderArtifact(artifact: Artifact, type: string) {
        let currentProject = type == TYPES.mainArtifact;

        let layer = null;
        if (currentProject) {
            layer = this.mainNodesLayer;
        } else if (type == TYPES.dependencyArtifact) {
            layer = this.dependenciesNodesLayer;
        } else {
            layer = this.consumedDependenciesNodesLayer;
        }


        let compo = layer.append("g").attr("class", ['artifact', type].join(' '));
        let rect = svg.builder.rect(compo, 1, 1, this.fontSize * 0.5, 'artifact');
        let text = svg.builder.text(compo, artifact.artifactId, artifact.groupId.split('.').join(' '), this.fontSize);



        let textSize = svg.math.size(text, this.container, true);
        rect.attr('width', textSize.width * this.fontPaddingX);
        rect.attr('height', textSize.height * this.fontPaddingY);


        svg.transform.center(text, rect, true, true);

        compo.on('mouseover', (d, i) => this.onHover(artifact.hash, d, i));
        compo.on('mouseout', (d, i) => this.onHoverExit(artifact.hash, d, i));


        this.artifactNodes[artifact.hash] = compo;
        if (currentProject) {
            this.mainArtifactNodes.push(compo);
        } else if (type == TYPES.dependencyArtifact) {
            this.dependenciesArtifactNodes.push(compo);
        } else {
            this.consumedArtifactNodes.push(compo);
        }


    }
    private renderConnectors() {
        if (isNotNull(this.innerValue.graph)) {
            for (let graph of this.innerValue.graph) {
                let dependencies = graph.dependencies;
                if (isNotNull(dependencies)) {
                    for (let dependency of dependencies) {
                        this.renderGraphConnector(graph.hash, dependency);
                    }
                }
            }
        }
    }

    private renderGraphConnector(fromArtifact: string, graph: ArtifactGraphDependency) {

        let toCompo = this.artifactNodes[fromArtifact];
        let fromCompo = this.artifactNodes[graph.hash];

        if (isNotNull(toCompo) && isNotNull(fromCompo)) {
            let posFromCompo = svg.math.size(fromCompo, this.svg, true);
            let posToCompo = svg.math.size(toCompo, this.svg, true);

            let linkDirectionY = posFromCompo.y > posToCompo.y ? 1 : -1;
            let linkDirectionX = posFromCompo.x > posToCompo.x ? 1 : -1;


            let points = [];
            let angleDiff = 0.03;


            let vector = {
                from: {
                    x: posFromCompo.x + (posFromCompo.width / 2),
                    y: posFromCompo.y + (posFromCompo.height / 2)
                },
                to: {
                    x: posToCompo.x + (posToCompo.width / 2),
                    y: posToCompo.y + (posToCompo.height / 2)
                }
            }

            let samePoint = fromArtifact == graph.hash;
            let angle = svg.math.vectorAngle(vector.from,vector.to);
            let angleSquare =samePoint?0: parseFloat(((angle/90) - Math.floor(angle/90)).toFixed(2));

            let halfPosFromCompoWidth= posFromCompo.width / 2


            if (!samePoint && (isNaN(angleSquare) || angleSquare<=(angleDiff) || angleSquare>=(1-(angleDiff)))) {

                points = [
                    { x: vector.from.x, y: vector.from.y },
                    { x: vector.to.x, y: vector.to.y },
                ];
            } else {
                let verticalAlign = angleSquare >=0.90;

                let vectorRatio = 4;
                let angle = -35;

                if(samePoint){
                    vectorRatio = vectorRatio*-0.25;
                    angle= 45;
                }
                if(verticalAlign){
                    if (linkDirectionX == 1) {
                        vector.from.x = vector.from.x - (halfPosFromCompoWidth);
                        vector.to.x = vector.to.x + (posToCompo.width / 2);
                    } else {
                        vector.from.x = vector.from.x - (halfPosFromCompoWidth);
                        vector.to.x = vector.to.x - (posToCompo.width / 2);
                    }
                }else{
                    if (linkDirectionX == 1) {
                        vector.from.x = vector.from.x - (halfPosFromCompoWidth);
                        if(samePoint){
                            vector.to.x = vector.from.x;
                        }else{
                            vector.to.x = vector.to.x + (posToCompo.width / 2);
                        }

                    } else {
                        vector.from.x = vector.from.x + (halfPosFromCompoWidth);

                        if(samePoint){
                            vector.to.x = vector.from.x;
                        }else{
                            vector.to.x = vector.to.x - (posToCompo.width / 2);
                        }
                    }
                }

                let curve = null;
                let curve2 = null;
                if(samePoint){
                    curve = svg.math.rotateByRef(vector.from.x, vector.from.y,
                        (vector.from.x - (halfPosFromCompoWidth / vectorRatio)),
                        (vector.from.y - ((vector.from.y - vector.to.y) / vectorRatio)),
                        angle * linkDirectionY);

                    curve2 = svg.math.rotateByRef(vector.to.x, vector.to.y,
                        (vector.to.x - (halfPosFromCompoWidth / vectorRatio)),
                        (vector.to.y - ((vector.to.y - vector.from.y) / vectorRatio)),
                        -angle * linkDirectionY);
                }else{
                    curve = svg.math.rotateByRef(vector.from.x, vector.from.y,
                        (vector.from.x - ((vector.from.x - vector.to.x) / vectorRatio)),
                        (vector.from.y - ((vector.from.y - vector.to.y) / vectorRatio)),
                        angle * linkDirectionY);

                    curve2 = svg.math.rotateByRef(vector.to.x, vector.to.y,
                        (vector.to.x - ((vector.to.x - vector.from.x) / vectorRatio)),
                        (vector.to.y - ((vector.to.y - vector.from.y) / vectorRatio)),
                        -angle * linkDirectionY);
                }


                points = [
                    { x: vector.from.x, y: vector.from.y },
                    { x: curve.x, y: curve.y, cmd: "C" },
                    { x: curve2.x, y: curve2.y },
                    { x: vector.to.x, y: vector.to.y }
                ];
            }

            let dependency = this.resolveDependencyConnection(graph.hash,fromArtifact);
            let justOne = dependency!=null && dependency.consume!=null && dependency.consume.length==1;

            let connector = svg.builder.pathNode(this.connectorsLayer, points, false, dependency==null|| dependency.consume==null?'':dependency.consume.join(' ')+(justOne?' just-one':'') );
            connector.attr("data-from", graph.hash);
            connector.attr("data-to", fromArtifact);
            connector.attr("data-angle", angle);

            let hitBox = svg.builder.pathNode(this.connectorsLayer, points, false, 'hit-box');
            hitBox.on('mouseover', (d, i) => this.onHoverConnector(connector, graph.hash, fromArtifact));
            hitBox.on('mouseout', (d, i) => this.onHoverConnectorExit(connector, graph.hash, fromArtifact));



            if (isNull(this.connectors[graph.hash])) {
                this.connectors[graph.hash] = [connector];
            } else {
                this.connectors[graph.hash].push(connector)
            }

        }
    }

    private positive(value: number): number {
        return value < 0 ? value * -1 : value;
    }
    /*****************************************************************************
    * MOVE NODES
    *****************************************************************************/
    private centerNodes() {
        this.centerNodesType(this.mainArtifactNodes, this.mainNodesLayer);
        this.centerNodesType(this.dependenciesArtifactNodes, this.dependenciesNodesLayer);
        this.centerNodesType(this.consumedArtifactNodes, this.consumedDependenciesNodesLayer);

        svg.transform.center(this.mainNodesLayer, this.svgContainer, true, true);

        let posMainNodeLayer = svg.math.size(this.mainNodesLayer, this.svgContainer, true);
        let posConsumedArtifactLayer = svg.math.size(this.consumedDependenciesNodesLayer, this.svgContainer, true);
        let posDpendenciesLayer = svg.math.size(this.dependenciesNodesLayer, this.svgContainer, true);


        svg.transform.translateX(this.consumedDependenciesNodesLayer, posMainNodeLayer.x - (posConsumedArtifactLayer.width + (this.width * this.columnMargin)));
        svg.transform.translateX(this.dependenciesNodesLayer, posMainNodeLayer.x + (posDpendenciesLayer.width + (this.width * this.columnMargin)));
    }

    private centerNodesType(nodes: any[], layer: any) {
        if (isNotEmpty(nodes)) {
            let bucketSize = this.resolveBucketSize(nodes);

            for (let i = 0; i < nodes.length; i++) {
                let posY = bucketSize * i;
                svg.transform.translateY(nodes[i], posY);
            }
        }
        svg.transform.center(layer, this.svgContainer, false, true);
    }

    private resolveBucketSize(nodes: any[]) {
        let node = nodes[0];
        let defaultBucketSize = this.width / nodes.length;

        let nodeSize = svg.math.size(node, this.container, true);
        let bucketWithMargin = nodeSize.height * this.nodeMargin;


        return defaultBucketSize < bucketWithMargin ? bucketWithMargin : defaultBucketSize;
    }

    private centerMainLayer() {
        svg.transform.scaleSameRatio(this.mainLayer, this.svg);
        svg.transform.center(this.svgContainer, this.svg, true, true);
    }

    /*****************************************************************************
    * RESOLVE
    *****************************************************************************/
    private resolveMainArtifact() {
        let result = {};
        for (let artifact of this.innerValue.artifacts) {
            if (isNotNull(artifact.currentProject) && artifact.currentProject) {
                result[artifact.hash] = artifact;
                this.mainArtifactsList.push(artifact);
            }
        }
        return result;
    }

    private resolveArtifactType(artifact: Artifact): string {
        let result = null;

        if (isNotNull(this.mainArtifacts[artifact.hash])) {
            result = TYPES.mainArtifact;
        } else {
            if (isNotNull(this.innerValue.graph)) {
                if (this.isDependency(artifact, this.mainArtifactsList, this.innerValue.graph)) {
                    result = TYPES.dependencyArtifact;
                } else {
                    result = TYPES.consumedArtifact;
                }
            } else {
                result = TYPES.dependencyArtifact;
            }
        }

        return result;
    }

    private isDependency(artifact: Artifact, mainArtifacts: Artifact[], graph: ArtifactGraph[]): boolean {
        let result = false;

        for (let mainArtifact of mainArtifacts) {
            let mainArtifactGraphs = graph.filter(g => g.hash == mainArtifact.hash);
            let mainArtifactGraph = isNull(mainArtifactGraphs) ? null : mainArtifactGraphs[0];

            if (isNotNull(mainArtifactGraph) && isNotEmpty(mainArtifactGraph.dependencies)) {
                for (let graphInfo of mainArtifactGraph.dependencies) {
                    if (artifact.hash == graphInfo.hash) {
                        return true;
                    }
                }
            }
        }

        return result;
    }

    /**************************************************************************
    * EVENTS
    **************************************************************************/
    private onHover(artifactHash: string, d: any, i: any) {
        let compo = this.artifactNodes[artifactHash];
        let connectors = this.connectors[artifactHash];

        if (isNotNull(compo)) {
            svg.builder.toggleStyleClass(compo, ON_HOVE);
        }

        if (isNotNull(connectors)) {
            for (let connector of connectors) {
                svg.builder.toggleStyleClass(connector, ON_HOVE);
            }
        }

    }
    private onHoverExit(artifactHash: string, d: any, i: any) {
        let compo = this.artifactNodes[artifactHash];
        let connectors = this.connectors[artifactHash];

        if (isNotNull(compo)) {
            svg.builder.toggleStyleClass(compo, ON_HOVE);
        }
        if (isNotNull(connectors)) {
            for (let connector of connectors) {
                svg.builder.toggleStyleClass(connector, ON_HOVE);
            }
        }
    }

    private onHoverConnector(connector: any, artifactHash: string, fromArtifact: string) {
        svg.builder.toggleStyleClass(connector, ON_HOVE);

        let dependency = this.resolveDependencyConnection(artifactHash,fromArtifact);

        this.toolTipsCompo.toggleStyleClass(ON_HOVE_TOOL_TIPS);
        this.toolTipsCompo.updateText(isNotNull(dependency)?dependency.consume.join(', '): '');
        this.toolTipsCompo.trackMouse();

    }
    private onHoverConnectorExit(connector: any, artifactHash: string, fromArtifact: string) {
        svg.builder.toggleStyleClass(connector, ON_HOVE);

        this.toolTipsCompo.toggleStyleClass(ON_HOVE_TOOL_TIPS);
        this.toolTipsCompo.untrackMouse();
    }


    private resolveDependencyConnection( artifactHash: string, fromArtifact: string):ArtifactGraphDependency{
        let dependency = null;
        let currentGraph = this.innerValue.graph.filter(g=> g.hash == fromArtifact);
        if(isNotEmpty(currentGraph)){
            let deps = currentGraph[0].dependencies.filter(d=> d.hash == artifactHash);
            if(isNotEmpty(deps)){
                dependency = deps[0];
            }
        }
        return dependency;
    }

    private onZoom(d:any){
        let ratio = 0.1;
        let delta =Math.round(Math.trunc(d.deltaY)/4)*ratio ;

        this.zoom = this.zoom-delta;

        if(this.zoom<0){
            this.zoom = ratio;
        }
        this.processZoom();
    }

    private processZoom(){
        svg.transform.scale(this.svgContainer, this.zoom, this.zoom);
    }
    private onMove(e){
        console.log('move')
    }

    private onDrag(event:any){
        this.mainLocatorPosition.x = this.mainLocatorPosition.x + (event.dx/this.zoom);
        this.mainLocatorPosition.y = this.mainLocatorPosition.y + (event.dy/this.zoom);
        svg.transform.translate(this.mainLocator, this.mainLocatorPosition.x , this.mainLocatorPosition.y);
    }

    private onDbclick(e){
        console.log(e)
        let zoomDelta = 1 - this.zoom;
        let xDelta = -this.mainLocatorPosition.x;
        let yDelta = -this.mainLocatorPosition.y;

        svg.animate.tween(this.mainLocator, 0, 1000, (t)=>{
                svg.transform.translate(this.mainLocator,
                                        this.mainLocatorPosition.x + (xDelta*t),
                                        this.mainLocatorPosition.y + (yDelta*t));


                let delta = (zoomDelta * t);
                let currentZoom = this.zoom + delta;
                svg.transform.scale(this.svgContainer, currentZoom, currentZoom);
        }, null,
        ()=>{
            this.mainLocatorPosition = {x:0, y:0};
            this.zoom = 1;
        });


    }
    /*****************************************************************************
    * IMPLEMENTS ControlValueAccessor
    *****************************************************************************/
    writeValue(value: any) {
        if (value !== this.innerValue) {
            this.hasValue = isNotNull(value) && isNotNull(value.artifacts);
            this.innerValue = value;
        }

        if (this.hasValue) {
            this.initializeGraph();
        } else {
            this.cleanGraph();
        }
    }

    registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }
    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }
}