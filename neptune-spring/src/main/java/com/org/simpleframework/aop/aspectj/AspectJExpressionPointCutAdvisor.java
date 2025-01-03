package com.org.simpleframework.aop.aspectj;

import com.org.simpleframework.aop.PointCut;
import com.org.simpleframework.aop.PointCutAdvisor;
import com.org.simpleframework.injection.annotation.Autowired;
import org.aopalliance.aop.Advice;

/**
 * <h2>低级切面</h2>
 * <h3>仅包含单个切入点和单个通知</h3>
 */
public class AspectJExpressionPointCutAdvisor implements PointCutAdvisor {

    private volatile AspectJExpressionPointCut pointCut;

    @Autowired
    private Advice advice;

    private String expression;

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public PointCut getPointCut() {
        if (this.pointCut == null){
            synchronized (AspectJExpressionPointCutAdvisor.class){
                if (this.pointCut == null)
                    return pointCut = new AspectJExpressionPointCut(this.expression);
            }
        }
        return pointCut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    public void setAdvice(Advice advice) {
        this.advice = advice;
    }
}
