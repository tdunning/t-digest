read.data = function(experiment, hash) {
    assign("watermark", sprintf("%s / %s", experiment, hash), envir=.GlobalEnv)
    assign("data", read.csv(sprintf("tests/accuracy-%s-%s.csv", experiment, hash)), envir=.GlobalEnv)
    # this makes a nice mark for plotting
    data$lx = round(log10(data$q.raw/(1-data$q.raw)))

    assign("sizes", read.csv(sprintf("tests/accuracy-sizes-%s-%s.csv", experiment, hash)), envir=.GlobalEnv)
    sizes$center = with(sizes, (q.0 + q.1)/2)

    assign("cdf", read.csv(sprintf("tests/accuracy-cdf-%s-%s.csv", experiment, hash)), envir=.GlobalEnv)
}

#buckets = read.csv("accuracy-samples.csv")

plot.cdf = function(sortFlag="sorted", compression=100, dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1, add=F, col=rgb(1,0,0,alpha=alpha), bufferSize = NA, offset=0, ylim=c(-0.08,0.08), bars=5) {
    i = cdf$digest == digest & cdf$dist == dist & cdf$sort == sortFlag & cdf$compression == compression
    if (!is.na(bufferSize)) {
        i = i & cdf$bufferSize == bufferSize
    }
    n = length(table(cdf$q[i]))
    boxplot(error ~ q, cdf[i,], add=add, col=col, boxwex=1/(bars+1), at=1:n + offset/bars, xaxt='n', ylim=ylim)
    axis(side=1, at=1:n, labels=levels(factor(cdf$q[i])))
    title(watermark, line=-1-offset)
}

plot.fill = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1, compression=100) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    plot(count*compression/1e6/pi*2 ~ center, sizes[i,], pch=21, bg=rgb(0,0,0,alpha=alpha), col=NA, cex=cex)
    title(watermark, line=-2)
}

plot.dk = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    plot(dk ~ center, sizes[i,])
    title(watermark, line=-2)
}

plot.buckets = function(sortFlag="sorted", alpha = 0.4, clusters=1:7, emphasis=c(), xlim=c(0, 0.003), dx=0.000025, generation=3, digest="MERGE") {
    x.max = 0
    for (ix in clusters) {
        i = with(buckets, digest == digest & dist == "UNIFORM" & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        zx = buckets[i,]$x
        x.max = max(c(x.max, zx))
    }
    b = seq(0,1.1 * x.max,by=dx)

    colorIndex = rep(1:4, len=length(clusters))
    colors = c(rgb(1,0,0,alpha=alpha), rgb(0,1,0,alpha=alpha), rgb(0,0,1,alpha=alpha), rgb(0.5, 0.5, 0.5,alpha=alpha))[colorIndex]
    addFlag = F
    for (ix in rev(clusters)) {
        i = with(buckets, digest == digest & dist == "UNIFORM" & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        zx = buckets[i,]$x
        hist(zx, xlim=xlim, add=addFlag, breaks=b, col=colors[abs(ix)], border=rgb(0,0,0,alpha=0.1))
        addFlag = T
    }
    for (ix in emphasis) {
        i = with(buckets, digest == digest & dist == "UNIFORM" & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        data = buckets[i,]$x
        hist(data, add=T, breaks=b, col=colors[ix], border=colors[ix])
    }        
    title(watermark, line=-2)
}
