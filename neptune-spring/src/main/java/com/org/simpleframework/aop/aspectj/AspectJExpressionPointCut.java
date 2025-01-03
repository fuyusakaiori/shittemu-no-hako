package com.org.simpleframework.aop.aspectj;

import com.org.simpleframework.aop.ClassFilter;
import com.org.simpleframework.aop.MethodMatcher;
import com.org.simpleframework.aop.PointCut;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * <h2>AspectJ 表达式仅支持方法增强</h2>
 */
public class AspectJExpressionPointCut implements PointCut, ClassFilter, MethodMatcher {

    /**
     * <h3>支持的原语, 也就是支持哪种类型的表达式解析</h3>
     */
    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<>();

    private final PointcutExpression pointcutExpression;

    // 初始化可以执行的原语
    static {
        // 1. 支持切点表达式采用 execution
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
        // 2. 支持切点表达式采用 annotation
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ANNOTATION);
    }

    /**
     * <h3>调用织入工具类解析表达式</h3>
     * @param expression 切点表达式
     */
    public AspectJExpressionPointCut(String expression) {
        // 1. 获取解析器
        PointcutParser parser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
                SUPPORTED_PRIMITIVES, this.getClass().getClassLoader());
        // 2. 解析器解析切点表达式
        pointcutExpression = parser.parsePointcutExpression(expression);
    }

    @Override
    public boolean matches(Class<?> clazz) {
        // 切点表达式就会查找当前是否有这个需要织入的类
        return pointcutExpression.couldMatchJoinPointsInType(clazz);
    }

    @Override
    public boolean matches(Method method, Class<?> clazz) {
        return pointcutExpression.matchesMethodExecution(method).alwaysMatches();
    }

    @Override
    public ClassFilter getClassFilter() {
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return this;
    }
}
