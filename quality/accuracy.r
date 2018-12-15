read.data = function(experiment, hash) {
    assign("watermark", sprintf("%s / %s", experiment, hash), envir=.GlobalEnv)
    assign("data", read.csv(sprintf("tests/accuracy-%s-%s.csv", experiment, hash)), envir=.GlobalEnv)
    # this makes a nice mark for plotting
    data$lx = round(log10(data$q.raw/(1-data$q.raw)))

    assign("sizes", read.csv(sprintf("tests/accuracy-sizes-%s-%s.csv", experiment, hash)), envir=.GlobalEnv)
    evalq(sizes$center <- with(sizes, (q.0 + q.1)/2), envir=.GlobalEnv)

    assign("cdf", read.csv(sprintf("tests/accuracy-cdf-%s-%s.csv", experiment, hash)), envir=.GlobalEnv)
}

read.slow.data = function() {
    assign("buckets", read.csv("accuracy-samples.csv"), envir=.GlobalEnv)
}

plot.cdf = function(sortFlag="sorted", compression=100, dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1, add=F, col=rgb(1,0,0,alpha=alpha), bufferSize = NA, offset=0, bars=5, norm=T, watermark=NA, ...) {
    cat("foo", digest, dist, sortFlag, compression, bufferSize, "\n")
    i = cdf$digest == digest & cdf$dist == dist & cdf$sort == sortFlag & cdf$compression == compression
    cat(dim(cdf[i,]), "\n")
    if (!is.na(bufferSize)) {
        i = i & cdf$bufferSize == bufferSize
    }
    n = length(table(cdf$q[i]))
    if (norm) {
        boxplot(error ~ q, cdf[i,], add=add, col=col, boxwex=1/(bars+1), at=1:n + offset/bars, xaxt='n', ...)
    } else {
        boxplot(x.digest-x.raw ~ q, cdf[i,], add=add, col=col, boxwex=1/(bars+1), at=1:n + offset/bars, xaxt='n', ...)
    }
    axis(side=1, at=1:n, labels=levels(factor(cdf$q[i])))
    title(watermark, line=-1-offset)
}


plot.q = function(sortFlag="sorted", compression=100, dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1, add=F, col=rgb(1,0,0,alpha=alpha), bufferSize = NA, offset=0, ylim=c(-0.08,0.08), bars=5, norm=T, watermark=NA) {
    cat(digest, dist, sortFlag, compression, bufferSize, "\n")
    base = data %>% filter(digest == digest, cdf$dist == dist, cdf$sort == sortFlag, cdf$compression == compression)
    if (!is.na(bufferSize)) {
        base = base %>% filter(bufferSize == bufferSize)
    }
    print(dim(base))
    n = dim(base %>% group_by(q) %>% summarise())[1]
    cat('n = ', n, '\n')
    if (norm) {
        boxplot(error ~ q.raw, base, add=add, col=col, boxwex=1/(bars+1), at=1:n + offset/bars, xaxt='n', ylim=ylim)
    } else {
        boxplot(q.digest-q.raw ~ q.raw, base, add=add, col=col, boxwex=1/(bars+1), at=1:n + offset/bars, xaxt='n', ylim=ylim)
    }
    axis(side=1, at=1:n, labels=levels(factor(base$q)))
    title(watermark, line=-1-offset)
}

plot.fill = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025, cex=1, compression=100) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    plot(count*compression/1e6/pi*2 ~ center, sizes[i,], pch=21, bg=rgb(0,0,0,alpha=alpha), col=NA, cex=cex)
    title(watermark, line=-2)
}

plot.dk = function(sortFlag="sorted", dist="UNIFORM", digest="MERGE", alpha = 0.4, dx=0.000025) {
    i = sizes$digest == digest & sizes$dist == dist & sizes$sort == sortFlag
    plot(dk ~ center, sizes[i,], ylim=c(0,4), cex=0.2)
    title(watermark, line=-2)
}

# plot the actual samples in the first few clusters
plot.buckets = function(sortFlag="sorted", alpha = 0.4, clusters=1:7, emphasis=c(), alpha.emphasis = 1-(1-alpha)/2, xlim=c(0, 0.003), dx=0.000025, generation=3, digestName="MergingDigest-alternating-twoLevel", distName="UNIFORM") {
    x.max = 0
    for (ix in clusters) {
        i = with(buckets, digest == digestName & dist == distName & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        zx = buckets[i,]$x
        x.max = max(c(x.max, zx))
    }
    print(x.max)
    b = seq(0,1.1 * x.max,by=dx)

    colorIndex = rep(1:4, len=length(clusters))
    colors = c(rgb(1,0,0,alpha=alpha), rgb(0,1,0,alpha=alpha), rgb(0,0,1,alpha=alpha), rgb(0.5, 0.5, 0.5,alpha=alpha))[colorIndex]
    addFlag = F
    for (ix in rev(clusters)) {
        i = with(buckets, digest == digestName & dist == distName & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        zx = buckets[i,]$x
        hist(zx, xlim=xlim, add=addFlag, breaks=b, col=colors[abs(ix)], border=rgb(0,0,0,alpha=0.1), xlab='q', main=NA)
        addFlag = T
    }
    print("emphasizing")
    colors = c(rgb(1,0,0,alpha=alpha.emphasis), rgb(0,1,0,alpha=alpha.emphasis), rgb(0,0,1,alpha=alpha.emphasis), rgb(0.5, 0.5, 0.5,alpha=alpha.emphasis))[colorIndex]
    for (ix in emphasis) {
        i = with(buckets, digest == digestName & dist == distName & k == generation & sort == sortFlag)
        if (ix >= 0) {
            i = i & (buckets$centroid == ix)
        } else {
            i = i & (buckets$centroid == (max(buckets$centroid[i]) + ix + 1))
        }
        data = buckets[i,]$x
        hist(data, add=T, breaks=b, col=colors[ix], border=colors[ix])
    }        
#    title(watermark, line=-2)
}

draw.bucket.spread = function() {
    ## the only figure we need from all this work is the one that shows how little clusters overlap
    buckets = read.csv("accuracy-samples.csv")
    pdf("cluster-spread.pdf", height=3, width=4)
    par(mar=c(5.1, 4.1, 1, 2.1))
    plot.buckets(digest="MergingDigest-K_2-kSize-alternating-twoLevel", clusters=1:20, xlim=c(0,0.001), sortFlag='unsorted', emphasis=c(13,15,17), alpha=0.4, alpha.emphasis=0.2)
    dev.off()
}

draw.error.figures = function() {
    pdf("relative-error-kSize.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="kSize", norm=T)
    dev.off()

    pdf("relative-error-weight.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="weight", norm=T)
    dev.off()

    pdf("absolute-error-kSize.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="kSize", norm=F, ylim=c(-3e-4, 3e-4))
    dev.off()

    pdf("absolute-error-weight.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="weight", norm=F, ylim=c(-3e-4, 3e-4))
    dev.off()
}

draw.error = function(bound="kSize", norm=T, ylim=c(-0.1, 0.1)) {
    d = function(digest, bound) {paste("MergingDigest-", digest, "-", bound, "-alternating-twoLevel", sep="")}
    par(mar=c(4.8, 4.4, 1, 2.1))
    par(cex.axis=0.75)
    par(cex.lab=1.2)
    par(mgp=c(3.1,0.7,0))
    par(las=2)
    plot.cdf(sortFlag="unsorted", digest=d("K_1", bound), ylim=ylim, norm=norm, offset=-1, xlim=c(0,14), ylab="Absolute Error in q", xlab="q")
    plot.cdf(sortFlag="unsorted", digest=d("K_2", bound), ylim=ylim, norm=norm, offset=0, add=T, col=rgb(0,1,0,alpha=0.4))
    plot.cdf(sortFlag="unsorted", digest=d("K_3", bound), norm=norm, offset=1, add=T, col=rgb(0,0,1,alpha=0.4))

    legend.y = max(ylim) * 0.95
    legend(12, legend.y, fill=c(rgb(1,0,0,alpha=0.4), rgb(0,1,0,alpha=0.4), rgb(0,0,1,alpha=0.4)), legend=c(expression(k[1]), expression(k[2]), expression(k[3])))
    abline(h=0.02, col=rgb(0,0,0,alpha=0.2))
    abline(h=0, col=rgb(0,0,0,alpha=0.2))
    abline(h=-0.02, col=rgb(0,0,0,alpha=0.2))
}


draw.details = function() {
    library(dplyr)
    base = buckets %>% filter(digest == "MergingDigest-K_3-weight-alternating-twoLevel" & dist == "UNIFORM" & x < 2e-3 & k == 0)
    bwplot(centroid ~ log10(x), data=base, panel = function(..., box.ratio) {
        panel.violin(..., col="transparent", varwidth=F, box.ratio, box.ratio, adjust=0.6)
        panel.dotplot(..., fill = NULL, box.ratio = .1, col=rgb(0,0,0,alpha=0.2), cex=0.4)
    })
}

logFloor = function(x) {
    10^(floor(log10(x)))
}

minmax = function(x) {
    c(min(x), max(x))
}

draw.relative.error.fig = function(xlim=c(0.5,7.4)) {
    pdf(file="relative-error.pdf", width=5, height=2.3, pointsize=9)
    layout(matrix(c(1,2), nrow=1))
    par(las=2)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.6, 0.5, 0))
    par(tcl=-0.3)
    ticks = seq(-60, 60, by=20)
    plot.cdf(digest="MergingDigest-K_1-weight-alternating-twoLevel", compression=100, norm=F, sortFlag="unsorted", offset=-1, cex=0.4, ylim=minmax(ticks*1e-6*1.2), xlim=xlim, xlab='q', ylab="Absolute error (ppm)", yaxt='n', angle=45)
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", compression=100, norm=F, sortFlag="unsorted", offset=0, col=rgb(0,1,0,alpha=0.4), add=T, yaxt='n')
    plot.cdf(digest="MergingDigest-K_3-weight-alternating-twoLevel", compression=100, norm=F, sortFlag="unsorted", offset=1, col=rgb(0,0,1,alpha=0.4), add=T, yaxt='n')

    axis(side=2, at=ticks*1e-6, labels=ticks)

    legend(0.5, -48e-6, 
           legend=c(expression(k[1]),expression(k[2]),expression(k[3])),
           fill=c(rgb(1,0,0,alpha=0.4), rgb(0,1,0,alpha=0.4), rgb(0,0,1,alpha=0.4)),
           cex=0.8, horiz=T)


    par(mar=c(3.9, 4.0, 0.5, 0.2))
    plot.cdf(digest="MergingDigest-K_1-weight-alternating-twoLevel", compression=100, norm=T, sortFlag="unsorted", offset=-1, cex=0.4, ylim=c(-0.3,0.3), xlim=xlim, xlab='q', ylab="Relative error")
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", compression=100, norm=T, sortFlag="unsorted", offset=0, add=T, col=rgb(0,1,0,alpha=0.4))
    plot.cdf(digest="MergingDigest-K_3-weight-alternating-twoLevel", compression=100, norm=T, sortFlag="unsorted", offset=1, add=T, col=rgb(0,0,1,alpha=0.4))

    legend(3, -0.2, 
           legend=c(expression(k[1]),expression(k[2]),expression(k[3])),
           fill=c(rgb(1,0,0,alpha=0.4), rgb(0,1,0,alpha=0.4), rgb(0,0,1,alpha=0.4)),
           cex=0.8, horiz=T)
    dev.off()
}

draw.accuracy.fig = function() {
    require(dplyr)
    pdf(file="error-vs-compression.pdf", width=4, height=3, pointsize=9)
    par(las=2)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.2, 0.2, 0.2))
    par(mgp=c(3.0, 0.5, 0))
    par(tcl=-0.3)

    plot.data = cdf %>% filter(digest=="MergingDigest-K_2-weight-alternating-twoLevel",  dist=="UNIFORM")   %>% group_by(compression, q)   %>% summarise(err=mean(abs(x.digest-x.raw)), x.digest=mean(x.digest), x.raw=mean(x.raw), clusters=mean(clusters))     %>% select(q, compression, x.digest, x.raw, err, clusters)    

    plot(err ~ compression, plot.data %>% filter(q==0.01), type='b', xlim=c(90,1050), ylim=c(0.02e-5,4e-5), log='xy', 
         xlab=expression(delta), ylab="Mean absolute error")
    lines(err ~ compression, plot.data %>% filter(q==0.001), type='b', ylim=c(0,100e5), col='red')
    lines(err ~ compression, plot.data %>% filter(q==0.0001), type='b', col='green')
    lines(err ~ compression, plot.data %>% filter(q==0.00001), type='b', col='blue')
    text(150, 6e-7, expression(q==10^-5))
    text(300, 1.6e-6, expression(q==10^-4))
    text(400, 3.5e-6, expression(q==10^-3))
    text(500, 1.2e-5, expression(q==0.01))

    delta = c(100,200,500,1030)
    polygon(c(delta,rev(delta)), c(3.05e-3/(delta), rev(3.05e-4/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)
    polygon(c(delta,rev(delta)), c(5.8e-4/(delta), rev(5.8e-5/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)
    polygon(c(delta,rev(delta)), c(2e-4/delta, rev(2e-5/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)
    polygon(c(delta,rev(delta)), c(5.25e-5/delta, rev(5.25e-6/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)
    dev.off()
}
