/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;


import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.annotations.FeignClientDefinition;
import io.inugami.maven.plugin.analysis.annotations.UsingFeignClient;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.getAnnotation;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.ifHasAnnotation;

@Slf4j
public class SpringFeignClientAnalyzer extends SpringRestControllersAnalyzer implements ClassAnalyzer {
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.feign";
    public static final String FEATURE      = FEATURE_NAME + ".enable";
    public static final String CONSUME      = "CONSUME";
    public static final String NONE_CLASS   = "$none";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && (getAnnotation(clazz, FeignClient.class) != null || getAnnotation(clazz, UsingFeignClient.class) != null);
    }

    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        final List<JsonObject> result = new ArrayList<>();

        if (getAnnotation(clazz, FeignClient.class) != null) {
            final List<JsonObject> subResult = super.analyze(clazz, context);
            if (subResult != null) {
                result.addAll(subResult);
            }
        } else if (getAnnotation(clazz, UsingFeignClient.class) != null) {
            final List<Class<?>> feignClasses = resolveFeignClasses(clazz);
            for (final Class<?> feignClientClass : feignClasses) {
                final List<JsonObject> subResult = super.analyze(feignClientClass, context);
                if (subResult != null) {
                    result.addAll(subResult);
                }
            }
        }


        return result;
    }

    private List<Class<?>> resolveFeignClasses(final Class<?> clazz) {
        final Set<Class<?>>    result                      = new LinkedHashSet<>();
        final UsingFeignClient feignClientConfigAnnotation = getAnnotation(clazz, UsingFeignClient.class);


        final Method[] methods = feignClientConfigAnnotation.feignConfigurationBean().getDeclaredMethods();
        for (final Method method : methods) {


            final FeignClientDefinition feignClient = getAnnotation(method, FeignClientDefinition.class);
            if (feignClient != null) {
                final Class<?> currentClass = feignClient.value();
                if (currentClass.getName().toLowerCase().endsWith(NONE_CLASS)) {
                    result.add(method.getReturnType());
                } else {
                    result.add(feignClient.value());
                }
            }
        }

        return new ArrayList<>(result);
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    @Override
    protected String getApiName(final Class<?> clazz) {
        return ifHasAnnotation(clazz, FeignClient.class, FeignClient::name, () -> clazz.getSimpleName());
    }

    @Override
    protected String getBaseContext(final Class<?> clazz) {
        String result = ifHasAnnotation(clazz, FeignClient.class, FeignClient::path);

        RequestMapping annotation = null;
        if (result == null) {
            annotation = getAnnotation(clazz, RequestMapping.class);
            if (annotation == null) {
                annotation = searchRequestMappingInInterface(clazz.getInterfaces());
            }
        }
        if (annotation != null && annotation.path().length > 0) {
            result = annotation.path()[0];
        }
        return result;
    }

    @Override
    protected String getRelationshipType() {
        return CONSUME;
    }
}
