### We want a piece-wise approximation of asin(x)
### But we want to have the following constraints:
### 1) each should be completely well behaved in its range
### 2) adjacent pieces will be blended using linear approximation so their regions should overlap
### 3) the blended result should have continuity
### 3) symmetry will be handled outside this approximation
### 4) the overall range handled should start at 0 and end before 1
### 5) the overall should be as large as possible, but need not reach 1

fit = function(param) {
    c0.high = param[1]
    c1.high = param[2]
    c2.low = param[3]
    c2.high = param[4]
    c3.low = param[5]
    c3.high = param[6]
    c4.low = param[7]

    x = seq(-c0.high, c0.high, by=0.01)
    d0 = data.frame(y=asin(x), x=x, x2=x*x, x3=x*x*x, i1=1/(1-x), i2=1/(1-x)/(1-x))
    m0 = glm(y ~ x + x2 + x3 + i1 + i2, d0, family='gaussian')

    x = seq(0, c1.high, by=0.01)
    d1 = data.frame(y=asin(x), x=x, x2=x*x, x3=x*x*x, i1=1/(1-x), i2=1/(1-x)/(1-x))
    m1 = glm(y ~ x + x2 + x3 + i1 + i2, d1, family='gaussian')

    x = seq(c2.low, c2.high, by=0.01)
    d2 = data.frame(y=asin(x), x=x, x2=x*x, x3=x*x*x, i1=1/(1-x), i2=1/(1-x)/(1-x))
    m2 = glm(y ~ x + x2 + x3 + i1 + i2, d2, family='gaussian')
    
    x = seq(c3.low, c3.high, by=0.01)
    d3 = data.frame(y=asin(x), x=x, x2=x*x, x3=x*x*x, i1=1/(1-x), i2=1/(1-x)/(1-x))
    m3 = glm(y ~ x + x2 + x3 + i1 + i2, d3, family='gaussian')

    list(m0=m0,m1=m1,m2=m2,m3=m3,
         c0.high=c0.high,c1.high=c1.high, c2.low=c2.low, c2.high=c2.high,
         c3.low=c3.low, c3.high=c3.high, c4.low=c4.low)
}

follow = function(models) {
    x = seq(0, models$c3.high, by=0.01)
    data = data.frame(x=x, x2=x*x, x3=x*x*x, i1=1/(1-x), i2=1/(1-x)/(1-x))
    raw = data.frame(
        y0=predict(models$m0, newdata=data),
        y1=predict(models$m1, newdata=data),
        y2=predict(models$m2, newdata=data),
        y3=predict(models$m3, newdata=data),
        y4=asin(x)
    )

    ## c0.high, c1.high, c2.low, c1.high, c3.low, c2.high, c3.high, c4.low
    mix = with(models, {
         mix = matrix(0, nrow=dim(raw)[1], ncol=5)
         x0 = bound((c0.high - x) / c0.high)
         x1 = bound((c1.high - x) / (c1.high - c2.low));
         x2 = bound((c2.high - x) / (c2.high - c3.low));
         x3 = bound((c3.high - x) / (c3.high - c4.low));

         mix[, 1] = x0
         mix[, 2] = (1-x0) * x1
         mix[, 3] = (1-x1) * x2
         mix[, 4] = (1-x2) * x3
         mix[, 5] = 1-x3
         mix
     })

    data.frame(x=x, yhat=rowSums(raw * mix), y=asin(x))
}

bound = function(v) {
    over = v > 1
    under = v < 0
    v * (1-over) * (1-under) + over
}
    
