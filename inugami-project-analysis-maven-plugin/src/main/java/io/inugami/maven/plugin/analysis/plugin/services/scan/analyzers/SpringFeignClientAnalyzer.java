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
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class SpringFeignClientAnalyzer extends SpringRestControllersAnalyzer implements ClassAnalyzer {
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.feign";
    public static final String FEATURE      = FEATURE_NAME + ".enable";
    public static final String CONSUME      = "CONSUME";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && (clazz.getAnnotation(FeignClient.class) != null || clazz.getAnnotation(UsingFeignClient.class) != null);
    }

    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        final List<JsonObject> result = new ArrayList<>();

        if (clazz.getAnnotation(FeignClient.class) != null) {
            final List<JsonObject> subResult = super.analyze(clazz, context);
            if (subResult != null) {
                result.addAll(subResult);
            }
        } else if (clazz.getAnnotation(UsingFeignClient.class) != null) {
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
        final UsingFeignClient feignClientConfigAnnotation = clazz.getAnnotation(UsingFeignClient.class);


        final Method[] methods = feignClientConfigAnnotation.feignConfigurationBean().getDeclaredMethods();
        for (final Method method : methods) {
            final FeignClientDefinition feignClient = method.getAnnotation(FeignClientDefinition.class);
            if (feignClient != null) {
                final Class<?> currentClass = feignClient.value();
                if (currentClass == FeignClientDefinition.None.class) {
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
        return ReflectionService
                .ifHasAnnotation(clazz, FeignClient.class, FeignClient::name, () -> clazz.getSimpleName());
    }

    @Override
    protected String getBaseContext(final Class<?> clazz) {
        return ReflectionService.ifHasAnnotation(clazz, FeignClient.class, FeignClient::path);
    }

    @Override
    protected String getRelationshipType() {
        return CONSUME;
    }
}
