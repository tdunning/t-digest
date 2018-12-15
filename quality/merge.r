data = read.csv("merge.csv")

plotMerge = function(n, yaxt = 's') {
  if (yaxt == 'n') {
    ylab = NA
    old = par(mar=c(4.5,1,3,0.5))
  } else {
    ylab = "Error in quantile (ppm)"
    old = par(mar=c(4.5,5,3,0.5))
  }
  par(lwd=0.2)
  our.data = data %>% filter(type == "quantile", parts == n)
  boxplot(e1*1e6 ~ q, at=(1:6)-0.23, xaxt='n', boxwex=0.2, our.data,
          ylim=c(-3000, 3000), cex=0.5, yaxt = yaxt,
          col=rgb(0.95, 0.95, 0.95), xlab='Quantile (q)', ylab=ylab)
  boxplot(e3*1e6 ~ q, at=1:6, xaxt='n', boxwex=0.19, add=T, our.data,
          col=rgb(0.7, 0.7, 0.7), cex=0.5, yaxt = yaxt)
  boxplot(e2*1e6 ~ q, at=1:6+0.23, xaxt='n', boxwex=0.2, add=T, our.data,
          col=rgb(0.4, 0.4, 0.4), cex=0.5, yaxt = yaxt)
  axis(side=1, at=1:6, labels=c(0.001, 0.01, 0.1, 0.2, 0.3, 0.5))
  legend(0.5, -1700, expression(Direct(delta==100), Two-level(delta==200,100), Merged(delta==100,100)), fill = c(rgb(0.95, 0.95, 0.95), rgb(0.7, 0.7, 0.7), rgb(0.4, 0.4, 0.4)), cex=0.7)
  abline(h=0, col=rgb(0.4, 0.4, 0.4))
  title(paste(n, " parts"))
  box()
  par(old)
}

#setEPS()
pdf("merge.pdf", width=6, height=2.4, pointsize=9)
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1.15,1,1))

plotMerge(5, 's')
plotMerge(20, 'n')
plotMerge(100, 'n')

dev.off()
