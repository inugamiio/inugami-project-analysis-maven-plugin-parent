import * as d3 from 'd3';
import { isNotNull, isNull, asserts , isFalse} from './utils';
import { Position, Dimension, Size, TransformationInfo } from '../models/svg.models';
import { builder } from './svg.rendering.service.builder';
import { math } from './svg.rendering.service.math';
import { transform } from './svg.rendering.service.transform';
import { event } from './svg.rendering.service.event';

export const DEFAULT_FONT_SIZE = 12;
export const TWO_PI = 2 * Math.PI;
export const TWO_PI_RATIO = (2 * Math.PI) / 360;

export const svg = {
    // *****************************************************************************
    // SVG DATA
    // *****************************************************************************
    data: {
        point: (x: number, y: number): Position => {
            return { "x": isNull(x) ? 0 : x, "y": isNull(y) ? 0 : y };
        }
    },

    "math": math,
    "builder": builder,
    "transform": transform,
    "event": event,

}