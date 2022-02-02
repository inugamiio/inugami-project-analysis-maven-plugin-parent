/***************************************************************************
* CHECK
***************************************************************************/
export const isTrue = (value:boolean)=>{
    return value != undefined && value!=null && value;
}
export const isFalse = (value:boolean)=>{
    return !isTrue(value);
}

export const isNotNull = (value)=>{
    return value != undefined && value!=null;
}
export const isNull = (value)=>{
    return value == undefined || value==null;
}

export const isNotEmpty = (value)=>{
    return isNotNull(value) && value.length>0;
}
export const isEmpty = (value)=>{
    return !isNotEmpty(value);
}

export const differentialHasContent = (differential)=>{
    let result = false;
    if(isNotNull(differential)){
        result = isNotEmpty(differential.newValues) 
        ||isNotEmpty(differential.deletedValues)  
        ||isNotEmpty(differential.sameValues);
    }
    return result;
}

/***************************************************************************
* ASSERTS
***************************************************************************/
export const asserts = {
    notNull : (value)=>{
        if(isNull(value)){
            throw "value mustn't be null"
        }
    }
}
/***************************************************************************
* GENERATOR
***************************************************************************/
const buildBase64Part = ()=>{
    return (((1+Math.random())*0x10000)|0).toString(16).substring(1); 
}

export const buildUid = ()=>{
    return (buildBase64Part() + buildBase64Part() + "-" + buildBase64Part() + "-4" + buildBase64Part().substr(0,3) + "-" + buildBase64Part() + "-" + buildBase64Part() + buildBase64Part() + buildBase64Part()).toLowerCase();
}
