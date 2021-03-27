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
        boxplot(abs(error) ~ q, cdf[i,], add=add, col=col, boxwex=1/(bars+1),
                lwd=0.5, at=1:n + offset/bars, xaxt='n', ...)
    } else {
        boxplot(abs(x.digest-x.raw) ~ q, cdf[i,], add=add, col=col, boxwex=1/(bars+1),
                lwd=0.5, at=1:n + offset/bars, xaxt='n', ...)
    }
#    axis(side=1, at=1:n, labels=levels(factor(cdf$q[i])))
    axis(side=1, at=1:13, labels=names(table(cdf$q)))
    abline(v=1:13, col=rgb(0,0,0,alpha=0.1))
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

    grey0 = rgb(1,1,1, alpha=alpha)
    grey1 = rgb(0.95, 0.95, 0.95, alpha=alpha)
    grey2 = rgb(0.7,0.7,0.7, alpha=alpha)
    grey3 = rgb(0.2,0.2,0.2, alpha=alpha)

    colorIndex = rep(1:4, len=length(clusters))
    ##colors = c(rgb(1,0,0,alpha=alpha), rgb(0,1,0,alpha=alpha), rgb(0,0,1,alpha=alpha), rgb(0.5, 0.5, 0.5,alpha=alpha))[colorIndex]
    ##colors = c(grey0, grey1, grey2, grey3)
    colors = c(grey1, grey2, grey1, grey2)[colorIndex]
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
    ##colors = c(rgb(1,0,0,alpha=alpha.emphasis), rgb(0,1,0,alpha=alpha.emphasis), rgb(0,0,1,alpha=alpha.emphasis), rgb(0.5, 0.5, 0.5,alpha=alpha.emphasis))[colorIndex]
    grey0 = rgb(1,1,1, alpha=alpha.emphasis)
    grey1 = rgb(0.95, 0.95, 0.95, alpha=alpha.emphasis)
    grey2 = rgb(0.7,0.7,0.7, alpha=alpha.emphasis)
    grey3 = rgb(0.2,0.2,0.2, alpha=alpha.emphasis)
    colors = c(grey3, grey3, grey3, grey3)[colorIndex]
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
    pdf("cluster-spread.pdf", height=3, width=4)
    par(mar=c(5.1, 4.1, 1, 2.1))
    grey0 = rgb(1,1,1)
    grey1 = rgb(0.95, 0.95, 0.95)
    grey2 = rgb(0.7,0.7,0.7)
    grey3 = rgb(0.4,0.4,0.4)
    plot.buckets(digest="MergingDigest-K_2-weight-alternating-twoLevel", clusters=1:20, xlim=c(0,0.001), sortFlag='unsorted', emphasis=c(13,15,17), alpha=0.4, alpha.emphasis=0.2)
    dev.off()
}

draw.error.figures = function(compression=100) {
    pdf("relative-error-kSize.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="kSize", norm=T, compression=compression, ylim=c(-0.3,.3))
    dev.off()

    pdf("relative-error-weight.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="weight", norm=T, compression=compression, ylim=c(-0.3,.3))
    dev.off()

    pdf("absolute-error-kSize.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="kSize", norm=F, ylim=c(-150e-6, 150e-6), compression=compression)
    dev.off()

    pdf("absolute-error-weight.pdf", height=2.5, width=5.5, pointsize=9)
    draw.error(bound="weight", norm=F, ylim=c(-150e-6, 150e-6), compression=compression)
    dev.off()
}

draw.error = function(bound="kSize", norm=T, ylim=c(-0.1, 0.1), compression=100) {
    d = function(digest, bound) {paste("MergingDigest-", digest, "-", bound, "-alternating-twoLevel", sep="")}
    par(mar=c(4.8, 4.4, 1, 2.1))
    par(cex.axis=0.75)
    par(cex.lab=1.2)
    par(mgp=c(3.1,0.7,0))
    par(las=2)
    if (norm) {
        ylab = "Relative Error in q"
    } else {
        ylab = "Absolute Error in q"
    }
    plot.cdf(sortFlag="unsorted", digest=d("K_1", bound), ylim=ylim, norm=norm, offset=-1, xlim=c(0,14), ylab=ylab, xlab="q", compression=compression)
    plot.cdf(sortFlag="unsorted", digest=d("K_2", bound), ylim=ylim, norm=norm, offset=0, add=T, col=rgb(0,1,0,alpha=0.4), compression=compression)
    plot.cdf(sortFlag="unsorted", digest=d("K_3", bound), norm=norm, offset=1, add=T, col=rgb(0,0,1,alpha=0.4), compression=compression)

#    if (norm) {
#        axis(side=2, at=ticks*1e-6, labels=ticks)
#    } else {
#        axis(side=2, at=ticks, labels=ticks)
#    }
    

    legend.y = max(ylim) * 0.95
    legend(12, legend.y, fill=c(rgb(1,0,0,alpha=0.4), rgb(0,1,0,alpha=0.4), rgb(0,0,1,alpha=0.4)), legend=c(expression(k[1]), expression(k[2]), expression(k[3])), bg='white')
    if (norm) {
        legend(9,legend.y, bquote(delta==.(compression)), bg='white', box.col='black', adj=0.2)
    } else {
        legend(12,-legend.y/3, bquote(delta==.(compression)), bg='white', box.col='black', adj=0.2)
    }
    
    abline(h=0.02, col=rgb(0,0,0,alpha=0.2))
    abline(h=0, col=rgb(0,0,0,alpha=0.2))
    abline(h=-0.02, col=rgb(0,0,0,alpha=0.2))
}


draw.details = function() {
    library(dplyr)
    library(lattice)
    base = buckets %>% filter(digest == "MergingDigest-K_3-weight-alternating-twoLevel" & dist == "UNIFORM" & x < 2e-3 & k == 0)
    base$index = base$centroid %% 2 + base$centroid/100
    bwplot(index ~ log10(x), data=base, panel = function(..., box.ratio) {
        panel.violin(..., col="transparent", varwidth=F, box.ratio, box.ratio, adjust=0.6)
        panel.dotplot(..., fill = NULL, box.ratio = .1, col=rgb(0,0,0,alpha=0.2), cex=0.8)
    })
}

### plot the first few clusters to show how they do or don't
draw.overlap = function() {
    pdf("cluster-spread.pdf", width=3, height=1.15, pointsize=9, family="serif")
    par(mar=c(3., 2.8, 0.5, 0.6))
    library(dplyr)
    base = buckets %>% filter(digest == "MergingDigest-K_3-kSize-alternating-twoLevel" & dist == "UNIFORM" & x < 2e-3 & k == 0) %>% mutate(index = centroid %% 2)
    z = base %>% summarise(minx=min(x), maxx=max(x))
    minx = z$minx
    maxx = z$maxx
    par(mgp=c(1.6,0.5,0))
    plot(x=c(),y=c(),xlim=c(log10(minx/2),log10(2*maxx)),
         ylim=c(-1.3, 2.3), yaxt='n', ylab=NA,
         xaxt='n', xlab=expression(italic(q)),
         cex.lab=1.2)
    axis(side=1, las=1, at=c(-3,-4,-5,-6,-7), 
         labels=expression(10^-3,10^-4,10^-5,10^-6,10^-7), 
         cex=0.8, cex.lab=1.5, tcl=-0.3)
    axis(side=2, las=2, at=c(0,1), labels=c("Even", "Odd"), tcl=-0.3, cex=0.8)
    base %>% group_by(centroid) %>% filter(centroid < 17) %>% summarize(xx=show.cluster(x,index, centroid))
    dev.off()
}

### shows the extent of a cluster
show.cluster = function(x,index, centroid){
    m = length(x)
    shade = (floor(centroid[1]/2) %% 2)
    x = log10(x)
    if (m > 1) {
        v = density(x, kernel="gaussian", bw="SJ", adjust=1.5)
        n = length(v$x)
        y0 = index[1]
        v$y = 2 * v$y / max(v$y)

##        points(x, rep(y0+shade/8-1/16,m), cex=0.6, col=NA, bg=rgb(0,0,0,alpha=0.3), pch=21)
##        text(mean(x), shade/8 + 1.7*y0-0.43, centroid[1])
##        line.y = shade/8 + 1.35*y0-0.25
        points(x, rep(y0,m), cex=0.6, col=NA, bg=rgb(0,0,0,alpha=0.3), pch=21)
        text(mean(x), 2.8*y0-0.88, centroid[1], cex=0.8)
        line.y = 1.8*y0-0.4
        arrows(x0=min(x), x1=max(x), y0=line.y, y1=line.y, angle=90, length=0.02, code=3)
        mean(index)
    } else {
        y0 = index[1]
##        points(x, rep(y0+shade/8-1/16,m), cex=0.6, col=NA, bg=rgb(0,0,0,alpha=0.3), pch=21)
##        text(x[1], shade/8 + 1.55*y0-0.35, centroid[1])
        points(x, rep(y0,m), cex=0.6, col=NA, bg=rgb(0,0,0,alpha=0.3), pch=21)
        print(y0)
        text(x[1], 2.8*y0-0.88, centroid[1], cex=0.8)
    }
    length(x)
}


logFloor = function(x) {
    10^(floor(log10(x)))
}

minmax = function(x) {
    c(min(x), max(x))
}

draw.relative.error.fig = function(xlim=c(0.5,7.4), compression=100) {
    pdf(file="relative-error.pdf", width=5, height=2.3, pointsize=9, family="serif")
    layout(matrix(c(1,2), nrow=1))
    par(las=2)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.6, 0.5, 0))
    par(tcl=-0.3)
    ticks = seq(-150, 150, by=50)
    grey1 = rgb(0.95, 0.95, 0.95)
    grey2 = rgb(0.7,0.7,0.7)
    grey3 = rgb(0.4,0.4,0.4)
    plot.cdf(digest="MergingDigest-K_1-weight-alternating-twoLevel", compression=compression, norm=F, sortFlag="unsorted", offset=-1, cex=0.4, ylim=minmax(ticks*1e-6*1.2), xlim=xlim, xlab=expression(italic(q)), ylab="Absolute error (ppm)", col=grey1, yaxt='n', angle=45)
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", compression=compression, norm=F, sortFlag="unsorted", offset=0, col=grey2, add=T, yaxt='n')
    plot.cdf(digest="MergingDigest-K_3-weight-alternating-twoLevel", compression=compression, norm=F, sortFlag="unsorted", offset=1, col=grey3, add=T, yaxt='n')

    axis(side=2, at=ticks*1e-6, labels=ticks)

    legend(0.5, -120e-6, 
           legend=c(expression(italic(k)[1]),expression(italic(k)[2]),expression(italic(k)[3])),
           fill=c(grey1, grey2, grey3),
           cex=0.8, horiz=T)


    par(mar=c(3.9, 4.0, 0.5, 0.2))
    plot.cdf(digest="MergingDigest-K_1-weight-alternating-twoLevel", compression=compression, norm=T, sortFlag="unsorted", offset=-1, cex=0.4, ylim=c(-0.3,0.3), xlim=xlim, xlab=expression(italic(q)), ylab="Relative error", col=grey1)
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", compression=compression, norm=T, sortFlag="unsorted", offset=0, add=T, col=grey2)
    plot.cdf(digest="MergingDigest-K_3-weight-alternating-twoLevel", compression=compression, norm=T, sortFlag="unsorted", offset=1, add=T, col=grey3)

    legend(3, -0.2, 
           legend=c(expression(italic(k)[1]),expression(italic(k)[2]),expression(italic(k)[3])),
           fill=c(grey1, grey2, grey3),
           cex=0.8, horiz=T)
    dev.off()
}

draw.relative.error.fig.single.panel = function(xlim=c(0.5,7.4)) {
    pdf(file="relative-error-one-panel.pdf", width=3, height=2.3, pointsize=9, family="serif")
    par(las=2)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.4, 0.5, 0))
    par(tcl=-0.3)
    ticks = seq(-150, 150, by=50)
    grey1 = rgb(0.95, 0.95, 0.95)
    grey2 = rgb(0.7,0.7,0.7)
    grey3 = rgb(0.4,0.4,0.4)

    plot.cdf(digest="MergingDigest-K_1-weight-alternating-twoLevel", 
             compression=100, norm=T, sortFlag="unsorted", offset=-1, 
             ylim=c(-0.3,0.3), xlim=xlim, xlab=expression(italic(q)), 
             ylab="Relative error", col=grey1, xaxt='n')
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", 
             compression=100, norm=T, sortFlag="unsorted", offset=0, add=T,
             col=grey2)
    plot.cdf(digest="MergingDigest-K_3-weight-alternating-twoLevel", 
             compression=100, norm=T, sortFlag="unsorted", offset=1, add=T,
             col=grey3)

    axis(side=1, at=1:7,
         labels=c(expression(10^-6), expression(10^-5), expression(10^-4), expression(10^-3), 0.01, 0.1, 0.5), las=1)
    legend(4, -0.2, 
           legend=c(expression(italic(k)[1]),
                    expression(italic(k)[2]),
                    expression(italic(k)[3])),
           fill=c(grey1, grey2, grey3),
           cex=0.8, horiz=T)
    dev.off()
}

draw.kll.comparison = function(xlim=c(0.5,7.4), compression=100, k.req=20, k.kll=200) {
    pdf(file="kll-comparison.pdf", width=5, height=2.3, pointsize=9, family="serif")
    layout(matrix(c(1,2), nrow=1))
    par(las=2)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.6, 0.5, 0))
    par(tcl=-0.3)
    ticks = seq(0, 200, by=50)
    grey1 = rgb(0.95, 0.95, 0.95)
    grey2 = rgb(0.7,0.7,0.7)
    grey3 = rgb(0.4,0.4,0.4)

    plot.req(req.k=k.kll, offset=-1, norm=F, col=grey1, alg='kll', ylim=c(0,max(ticks)*1e-6), ylab="Absolute Error (ppm)", outcex=0.5)
    
    plot.req(req.k=k.req, offset=0, norm=F, add=T, col=grey2, alg='req', outcex=0.5)
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", compression=compression, norm=F, sortFlag="unsorted", offset=1, add=T, col=grey3, yaxt='n', outcex=0.5)

    axis(side=2, at=ticks*1e-6, labels=ticks)

    # xpd allows plotting outside the plot window
    legend(7.88, -200e-6, 
           legend=c(expression("kll","req",italic(t)-"digest")),
           fill=c(grey1, grey2, grey3),
           cex=0.6, horiz=F, xpd=NA)


    par(mar=c(3.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.6, 0.5, 0))

    plot.req(req.k=k.kll, offset=-1, norm=T, alg='kll', ylab=NA, ylim=c(0,0.1), col=grey1, outcex=0.5)
    plot.req(req.k=k.req, offset=0, norm=T, add=T, alg='req', col=grey2, outcex=0.5)
    plot.cdf(digest="MergingDigest-K_2-weight-alternating-twoLevel", compression=compression, norm=T, sortFlag="unsorted", offset=1, add=T, col=grey3, outcex=0.5)
    mtext("Relative error", 2, line=2, cex=1.25, las=3)

    dev.off()
}

box.text = function(x, y, text, adj=0.5) {
    w = strwidth(text) * 1.1
    h = strheight(text) * 1.4
    padj = 0.5
    rect(x-w*adj, y-h*padj, x+w*(1-adj), y+h*(1-padj), col='white', bor=NA)
    text(x, y, labels=text, adj=adj)
}

draw.accuracy.fig = function() {
    require(dplyr)
    pdf(file="error-vs-compression.pdf", width=4, height=3, pointsize=9, family="serif")
    par(las=2)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.2, 0.2, 0.2))
    par(mgp=c(3.0, 0.5, 0))
    par(tcl=-0.3)


    plot.data = cdf %>% filter(digest=="MergingDigest-K_2-weight-alternating-twoLevel",  dist=="UNIFORM")   %>% group_by(compression, q)   %>% summarise(err=mean(abs(x.digest-x.raw)), x.digest=mean(x.digest), x.raw=mean(x.raw), clusters=mean(clusters))     %>% select(q, compression, x.digest, x.raw, err, clusters)    

    ymax = max((plot.data %>% filter(q==0.01))$err)
    ymin = max((plot.data %>% filter(q==0.00001))$err)

    plot(err ~ compression, plot.data %>% filter(q==0.01), type='b', xlim=c(40,1050),
         ylim=c(ymin/2,1.2*ymax), log='xy', 
         xlab=expression(delta), ylab="Mean absolute error")
    lines(err ~ compression, plot.data %>% filter(q==0.001), type='b', ylim=c(0,100e5), col='red')
    lines(err ~ compression, plot.data %>% filter(q==0.0001), type='b', col='green')
    lines(err ~ compression, plot.data %>% filter(q==0.00001), type='b', col='blue')


    y = 1.3 * (plot.data %>% filter(q==0.00001,compression==100))$err
    text(150, y, expression(italic(q)==10^-5))

    y = 1.5 * (plot.data %>% filter(q==0.0001,compression==200))$err
    text(200, y, expression(italic(q)==10^-4), adj=0)

    y = 1.9 * (plot.data %>% filter(q==0.001,compression==500))$err
    text(400, y, expression(italic(q)==10^-3), adj=0.75)

    y = 1.5 * (plot.data %>% filter(q==0.01,compression==500))$err
    text(500, y, expression(italic(q)==0.01), adj=0)

    delta = c(50, 100,200,500,1030)

    # show sqrt(delta) scaling lines
    y = ymin
    x = seq(20, 1500, by=100)
    while (y < 10 * ymax) {
        k = sqrt(x[1]) * y
        lines(x, k / sqrt(x), col='grey', lty=2)
        y = y * 2
    }
    # show delta^2 scaling lines
    y = ymin
    x = seq(20, 1500, by=100)
    while (y < 400 * ymax) {
        k = (x[1])^2 * y
        lines(x, k / (x)^2, col='lightgrey', lty=3)
        y = y * 2
    }

    if (FALSE) {
    y = (plot.data %>% filter(q==0.01,compression==100))$err
    k1 = delta[1] * y
    k2 = sqrt(delta[1]) * y
    polygon(c(delta,rev(delta)), c(k1/(delta), rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)

    y = (plot.data %>% filter(q==0.001,compression==100))$err
    k1 = delta[1] * y
    k2 = sqrt(delta[1]) * y
    polygon(c(delta,rev(delta)), c(k1/(delta), rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)

    y = (plot.data %>% filter(q==0.0001,compression==100))$err
    k1 = delta[1] * y
    k2 = sqrt(delta[1]) * y
    polygon(c(delta,rev(delta)), c(k1/delta, rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)

    y = (plot.data %>% filter(q==0.00001,compression==100))$err
    k1 = delta[1] * y
    k2 = sqrt(delta[1]) * y
    polygon(c(delta,rev(delta)), c(k1/delta, rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)
    }
    dev.off()
}

draw.accuracy.fig.small = function() {
    require(dplyr)
    pdf(file="error-vs-compression-small.pdf", width=3, height=2.25, pointsize=9, family="serif")
    par(las=1)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.5, 3.2, 0.2, 0.2))
    par(mgp=c(2.0, 0.5, 0))
    par(tcl=-0.3)

    plot.data = cdf %>% filter(digest=="MergingDigest-K_2-weight-alternating-twoLevel",  dist=="UNIFORM")   %>% group_by(compression, q)   %>% summarise(err=mean(abs(x.digest-x.raw)), x.digest=mean(x.digest), x.raw=mean(x.raw), clusters=mean(clusters))     %>% select(q, compression, x.digest, x.raw, err, clusters)    

    ymax = max((plot.data %>% filter(q==0.01))$err)
    ymin = max((plot.data %>% filter(q==0.00001))$err)

    plot(err ~ compression, plot.data %>% filter(q==0.01), type='b', 
         xlim=c(40,1050), ylim=c(ymin/2,1.2*ymax), log='xy', 
         xlab=NA, ylab=NA,
         yaxt='n')
    title(ylab=c("Absolute error (ppm)"), mgp=c(1.8, 0.5, 0))
    title(xlab=expression(delta), mgp=c(2.1, 0.5, 0))
    axis(side=1)
    axis(side=2, at=c(1e-6, 3e-6, 1e-5, 3e-5, 1e-4, 3e-4),
         labels=c(1, 3, 10, 30, 100, 300))
    lines(err ~ compression, plot.data %>% filter(q==0.001), type='b', ylim=c(0,100e5), col='red')
    lines(err ~ compression, plot.data %>% filter(q==0.0001), type='b', col='green')
    lines(err ~ compression, plot.data %>% filter(q==0.00001), type='b', col='blue')


    y = 1.3 * (plot.data %>% filter(q==0.00001,compression==100))$err
    text(150, y, expression(italic(q)==10^-5), cex=0.8)

    y = 1.3 * (plot.data %>% filter(q==0.0001,compression==200))$err
    text(240, y, expression(italic(q)==10^-4), adj=0, cex=0.8)

    y = 1.9 * (plot.data %>% filter(q==0.001,compression==500))$err
    text(500, y, expression(italic(q)==10^-3), adj=0.5, cex=0.8)

    y = 1.5 * (plot.data %>% filter(q==0.01,compression==500))$err
    text(500, y, expression(italic(q)==0.01), adj=0, cex=0.8)

    delta = c(50, 100,200,500,1030)

    # show sqrt(delta) scaling lines
    y = ymin
    x = seq(20, 1500, by=100)
    while (y < 10 * ymax) {
        k = sqrt(x[1]) * y
        lines(x, k / sqrt(x), col='grey', lty=2)
        y = y * 2
    }
    # show delta^2 scaling lines
    y = ymin
    x = seq(20, 1500, by=100)
    while (y < 400 * ymax) {
        k = (x[1])^2 * y
        lines(x, k / (x)^2, col='lightgrey', lty=3)
        y = y * 2
    }

    if (FALSE) {
        y = (plot.data %>% filter(q==0.01,compression==100))$err
        k1 = delta[1] * y
        k2 = sqrt(delta[1]) * y
        polygon(c(delta,rev(delta)), c(k1/(delta), rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)

        y = (plot.data %>% filter(q==0.001,compression==100))$err
        k1 = delta[1] * y
        k2 = sqrt(delta[1]) * y
        polygon(c(delta,rev(delta)), c(k1/(delta), rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)

        y = (plot.data %>% filter(q==0.0001,compression==100))$err
        k1 = delta[1] * y
        k2 = sqrt(delta[1]) * y
        polygon(c(delta,rev(delta)), c(k1/delta, rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)

        y = (plot.data %>% filter(q==0.00001,compression==100))$err
        k1 = delta[1] * y
        k2 = sqrt(delta[1]) * y
        polygon(c(delta,rev(delta)), c(k1/delta, rev(k2/sqrt(delta))), col=rgb(0,0,0,alpha=0.2), density=-1, border=NA)
    }
    dev.off()
}

plot.comparison = function(q.value=0.1) {
    grey1 = rgb(0.95, 0.95, 0.95)
    grey2 = rgb(0.7,0.7,0.7)
    grey3 = rgb(0.4,0.4,0.4)

    plot.error(k=8, alg='req', q=q.value, ylim=c(0,0.055), offset=-1, add=F, outcex=0.5, col=grey1)
    plot.error(k=200, alg='t2', q=q.value, offset=0, add=T, outcex=0, col=grey2)
    plot.error(k=1000, alg='t2', q=q.value, offset=1, add=T, outcex=0, col=grey3)

    legend("topleft", inset=0.02, legend=c("KLL-req (3.9kB)", "t-digest (1kB)", "t-digest (4.4kB)"), fill=c(grey1, grey2, grey3), bg=rgb(1,1,1,alpha=0.7), cex=0.9)
    text(0.5, 0.032, cex=1.2, adj=0, bquote(italic(q)==10^.(log10(q.value))))
}

draw.kll = function(file="kll-comparison.pdf", q.values=c(0.1, 0.01, 0.001, 0.0001)) {
    pdf(file=file, width=7, height=4.2, pointsize=10, family="serif")
    layout(matrix(c(1,2,3,4), nrow=2, byrow=T))
    par(las=2)
#    par(cex=0.65)
    par(cex.axis=0.9)
    par(cex.lab=1.3)
    par(mar=c(3.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.6, 0.5, 0))
    par(tcl=-0.3)

    plot.comparison(q.value=q.values[1])
    dev.next()
    plot.comparison(q.value=q.values[2])
    dev.next()
    plot.comparison(q.value=q.values[3])
    dev.next()
    plot.comparison(q.value=q.values[4])
    dev.off()
}

plot.error = function(k=10, alpha = 0.4, cex=1, add=F, norm=T, col=rgb(1,0,0,alpha=alpha), offset=0, bars=5, watermark=NA, alg=NA, q=0.01, ...) {
    require(dplyr)
    algorithm = alg
    k.value = k
    q.value = q
    req = read.csv("kll-accuracy.csv") %>% filter(k==k.value, q == q.value, alg==algorithm)
    n = length(table(req$n0))
    par(mar=c(2.9, 4.0, 0.5, 0.2))
    par(mgp=c(2.0, 0.4, 0))
    if (norm) {
        boxplot(rel.error ~ n0, req, type='b', boxwex=1/(bars+2), xlab=NA,
                at=(1:n)+offset/(bars), xlim=c(0, n)+0.5, xaxt='n',
                ylab="Relative Error",
                add=add, col=col, ...)
    } else {
        boxplot(abs.error*1e6 ~ n0, req, type='b', boxwex=1/(bars+2), xlab=NA,
                at=(1:n)+offset/(bars), xlim=c(0, n)+0.5, xaxt='n',
                ylab="Absolute Error (ppm)",
                add=add, col=col, ...)
    }
    values = sapply(as.numeric(names(table(req$n0))), function(n) {bquote(10^.(round(log10(n))))})
    values=as.vector(values, "expression")
    axis(1, at=1:n, labels=values, las=1)
    mtext(expression(italic(n)), side=1, line=1.5, las=1, cex=1.2)
}


plot.req = function(req.k=10, alpha = 0.4, cex=1, add=F, col=rgb(1,0,0,alpha=alpha), offset=0, bars=5, norm=T, watermark=NA, alg=NA, samples=100000, ...) {
    require(dplyr)
    algorithm = alg
    req = read.csv("kll-accuracy.csv") %>% filter(k==req.k, n==samples, alg==algorithm)
    print(table(req$n))

    n = length(table(req$q))
    if (norm) {
        boxplot(pmax(1e-10,rel.error) ~ q, req, add=add, col=col, boxwex=1/(bars+1),
                lwd=0.5, at=1:n + offset/bars, xaxt='n',
                xlab=expression(italic(q)), ...)
    } else {
        boxplot(abs.error ~ q, req, add=add, col=col, boxwex=1/(bars+1),
                lwd=0.5, at=1:n + offset/bars, xaxt='n', yaxt='n',
                xlab=expression(italic(q)), ...)
    }
    axis(side=1, at=1:n, labels=levels(factor(req$q)))
    abline(v=1:13, col=rgb(0,0,0,alpha=0.1))
}

