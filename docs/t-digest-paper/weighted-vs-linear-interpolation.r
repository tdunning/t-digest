### Compare linear interpolation of centroids to weighted interpolation ###
fade = rgb(0,0,0,alpha=0.5)
dot.size = 0.7
set.seed(5)

pdf("weighted-vs-linear-interpolation.pdf", width=6, height=2.7, pointsize=10)
layout(matrix(c(1,2),byrow=T, ncol=2), widths=c(1.1,1))
x = sort(log(1-runif(10000))) # sorted exponential distribution
F = ((0:(length(x)-1))+0.5)/length(x) # the y points for an x point to its percentile
par(mar=c(2.5,3,1,1))
plot(x, F, cex=dot.size, pch=21, bg=fade, col=NA, type='b', xlim=c(x[1], x[110]), ylim=c(0,0.01), xaxt='n', ylab=NA, mgp=c(1,0.5,0), xlab=NA)

axis(side=1, at=-10:-1, labels=NA)
title(xlab='x', line=0.8, cex.lab=1.5)
title(ylab='q', line=1.5, cex.lab=1.5)

left.end = min(x)

lines(c(left.end, x[100]), c(0, 0.01), lwd=2)
lines(c(left.end, left.end), c(-0.0005, 0.0005), lt=1, col='black', lwd=0.5)
lines(c(x[100], x[100]), c(0.0085, 0.015), lt=1, col='black', lwd=0.5)
text(-7, 0.006, "100")

q.to.k = function(q) {
    (asin(2*q-1)/pi + 1/2)
}

k.to.q = function(k,compression) {
    sin(k/compression*pi - pi/2)/2 + 0.5
}

# This function makes a plot of the cdf of the sorted distribution in the global variable "x"
# The graph limits are defined by x[rangeMin]/x[rangeMax], which are values from [1,n]. By default, it
# with only graph ~ the first percentile.
# It will then calculate the positions of the centroids based on the weights passed in.
# Lastly, it will graph these positions with draw function which is passed in
makeChart = function(weights, titleToDisp, drawFunc, rangeMin=1, rangeMax=round(1.1*length(x)/100)) {
    xLimits = c(x[rangeMin], x[rangeMax])
    yLimits = c((rangeMin-1)/length(x), rangeMax/length(x))
    F = ((0:(length(x)-1))+0.5)/length(x) # the y points for an x point to its percentile

    #plot the points of the distribution
    plot(x, F, cex=dot.size, pch=21, bg=fade, col=NA, type='b', xlim=xLimits, ylim=yLimits, xaxt='n')
    title(main=titleToDisp)
    axis(side=1, at=-10:-1, labels=NA)
    axis(side=2, at=(0:6)/10, labels=NA)
    title(xlab='x', line=0.8, cex.lab=1.5)
    title(ylab='q', line=2, cex.lab=1.5)

    xCoordinatesEnds = c(1, cumsum(weights), length(x)) # x[coordinateEnds[i]] are the end points of each centroid

    # xCoordinates[i] is the mean value of all of the points that in centroid[i]
    xCoordinates = numeric(length(xCoordinatesEnds))
    xCoordinates[1]=x[1] # first centroid is always the min
    for(i in 2:length(xCoordinates)) {
        xCoordinates[i] = mean(x[xCoordinatesEnds[i-1]:xCoordinatesEnds[i]])
    }

    # each centroid's height is all of the weight up to the centroid minus half the centroid weight.
    yCoordinates = c(0, cumsum(weights)- weights / 2, length(x)) / sum(weights)

    # note - weight of min and max is specified as 1. It could be k.to.q(1/compression) for better results.
    weightsAlignedByPoints = c(1,weights,1)

    # draw chart
    drawFunc(xCoordinates, yCoordinates, weightsAlignedByPoints)
    # write the weight of each centroid above it.
    text(xCoordinates, yCoordinates + (rangeMax-rangeMin)*.06/length(x), round(c(1, weights, 1)))
}

# Draw the centroids linearly interpolated
drawLinear = function(xCoordinates, yCoordinates, weights) {
    lines(xCoordinates, yCoordinates, type='o', lwd=1, col='blue')
}
# Draw the centroids interpolated by weight
drawWeighted = function(xCoordinates, yCoordinates, weights) {
    points(xCoordinates, yCoordinates, col='blue')
    for(i in 1:(length(xCoordinates)-1)) {
        w1 = weights[i]
        w2 = weights[i+1]
        x1 = xCoordinates[i]
        x2 = xCoordinates[i+1]
        mean1 = yCoordinates[i]
        mean2 = yCoordinates[i+1]

        # Weighted average with centroid weight as 
        weightFunc = function(q) {
            (mean1*w1*(x2-q)+mean2*w2*(q-x1)) / (w1*(x2-q)+w2*(q-x1))
        }
        curve(weightFunc, x1, x2, add=TRUE, col="blue")
    }
}

# This function calculates the ideal centroid weights for the specified compression / dataset size
getIdealBounds = function(n, compression) {
    leftBounds = c(0, k.to.q(1:(compression-1), compression))
    rightBounds = k.to.q(1:compression, compression)
    (rightBounds - leftBounds) * length(x) 
}

# these weights were taken from linear-interpolation.r
weights = c(2, 8, 19, 35, 56, 81, 111)

n=length(x)
makeChart(c(weights, n-sum(weights)), "Preset Weights", drawLinear)

# Show comparison for these preset weights
makeChart(c(weights, n-sum(weights)), "Preset Weights", drawLinear)
makeChart(c(weights, n-sum(weights)), "Preset Weights", drawWeighted)

drawExampleCharts = function(distName) {
    n = length(x)

    # make comparison charts for ideal centroid placements
    for (i in c(50,100,200)) {
        titleToDisp = paste(distName," c=", i)
        makeChart(getIdealBounds(n, i), titleToDisp, drawLinear)
        makeChart(getIdealBounds(n, i), titleToDisp, drawWeighted)
    }

    midLen = n/20 # 5%
    titleToDisp = paste(distName," c=", 100)

    # make chart of top 0.025-0.05 quantiles
    makeChart(getIdealBounds(n, 100), titleToDisp, drawLinear, rangeMin=round(midLen/2), rangeMax=midLen)
    makeChart(getIdealBounds(n, 100), titleToDisp, drawWeighted, rangeMin=round(midLen/2), rangeMax=midLen)

    # make chart of top 0.45-0.55 quantiles
    makeChart(getIdealBounds(n, 100), titleToDisp, drawLinear, rangeMin=round(n/2-midLen), rangeMax=round(n/2+midLen))
    makeChart(getIdealBounds(n, 100), titleToDisp, drawWeighted, rangeMin=round(n/2-midLen), rangeMax=round(n/2+midLen))


    # make chart of top 0.01 quantile
    bottomPercent = (n*1.1)/100 # 1.1% of elements
    makeChart(getIdealBounds(n, 100), titleToDisp, drawLinear, rangeMin=n-bottomPercent, rangeMax=n)
    makeChart(getIdealBounds(n, 100), titleToDisp, drawWeighted, rangeMin=n-bottomPercent, rangeMax=n)
}

drawExampleCharts(paste("Exp n=", length(x)))

x = sort(log(1-runif(50000))) # sorted exponential distribution
drawExampleCharts(paste("Exp n=", length(x)))

x = sort(rnorm(50000)) # sorted normal distribution
drawExampleCharts(paste("Normal n=", length(x)))

x = sort(rnorm(50000)) # sorted normal distribution
drawExampleCharts(paste("Normal n=", length(x)))

x = sort(runif(50000)) # sorted uniform distribution
drawExampleCharts(paste("Uniform n=", length(x)))

dev.off()
