### Draws a figure illustrating change detection in the distribution of synthetic data.
### Each dot represents a single time period with 1000 samples. Before the change,
### the data is sampled from a unit normal distribution. After the change, 20 samples
### in each time period are taken from N(3,1). Comparing counts with a chi^2 test that
### is robust to small expected counts robustly detects this shift.

### log-likelihood ratio test for multinomial data
llr = function(k) {
    2 * sum(k) * (H(k) - H(rowSums(k)) - H(colSums(k)))
}
H = function(k) {
    N = sum(k) ;
    return (sum(k/N * log(k/N + (k==0))))
}

### compare recent samples to historical by comparing counts in a range of interest
analyze = function(historical, recent, cuts) {
    counts = data.frame(
        a=hist(recent, breaks=cuts, plot=F)$counts, 
        b=hist(historical, breaks=cuts, plot=F)$counts)
    llr(counts)
}

### use fixed seed for stability of the pictures
set.seed(3)
### lots of reference data
historical = rnorm(100000)

### set cuts based on historical data
### in practical systems, this step would be implemented with a t-digest 
cuts = c(-10, quantile(historical, probs=c(0.99, 0.999)), 20)

### 1000 samples per time period, 2% perturbation after change
n = 1000
epsilon = 0.02

### sample 60 scores without perturbation
scores = rep(0,100)
for (i in 1:60) {
    scores[i] = analyze(historical, c(rnorm(n)), cuts)
}

### sample 40 scores with perturbation
for (i in 1:40) {
    scores[i + 60] = analyze(historical, c(rnorm(n * (1-epsilon)), rnorm(n * epsilon, 3)), cuts)
}

### plot the data
pdf("change-point.pdf", width=5, height=4, pointsize=10)
old = par('mgp')
par(mgp=c(3,0.6,0))
colors = c(rep(rgb(0,0,0,alpha=0.8),60), rep(rgb(1,0,0,alpha=0.8),40))
plot(scores, xaxt='n', xlab=NA, ylab=NA, ylim=c(0,60), cex=1.3, pch=21, bg=colors, col=NA)
abline(v=60.5, lwd=3, col=rgb(0,0,0, alpha=0.1))

polygon(c(-1,55,55,-1,-1), c(60, 60,36,36,60), col='white')
points(c(1.5, 1.5), c(55, 45), pch=21, bg=c('black', 'red'), col=NA)
text(5, c(55,45), adj=0, labels=c(
    expression(x %~% symbol(N)(mu == 0)), 
    expression(x %~% bgroup("[", atop(symbol(N)(mu==0) , symbol(N)(mu==3)),""))))
text(30, c(55,48,41.5), c("1000 samples", "980 samples", "20 samples"), adj=0)


mtext(expression(llr(counts)), side=2, padj=-1.3, cex=1.4)
mtext("Before change", at=25, side=1, padj=1, cex=1.5)
mtext("After change", at=75, side=1, padj=1, cex=1.5)
par(mgp=old)
dev.off()


