import os
import pandas as pd
import matplotlib.pyplot as plt

in_tree = "data/tree/"
out_tree = "plots/tree/"

in_merging = "data/merging/"
out_merging = "plots/merging/"


scale_function_prefixes = ["K_{0}_{1}".format(x, y) for x in ["1", "2", "3"] for y in
                           ["USUAL", "GLUED"]] + ["K_0_USUAL"] + ["K_QUADRATIC"]


def clean_string(s):
    return s.replace("K", "k").replace("_USUAL", "").replace("GLUED", "glued"). \
        replace("QUADRATIC", "quadratic")


cc_suffix = "_centroid_counts.csv"

axis_labels = {'.99': 2, '0.99': 2,
               '1.0E-5': -5, '0.00001': -5,
               '1.0E-4': -4, '0.0001': -4,
               '.99999': 5, '0.99999': 5,
               '.001': -3, '0.001': -3,
               '.9': 1, '0.9': 1,
               '.999': 3, '0.999': 3,
               '.9999': 4, '0.9999': 4,
               '.1': -1, '0.1': -1,
               '.01': -2, '0.01': -2,
               '.5': 0, '0.5': 0}


def generate_figures(prefixes=scale_function_prefixes, save=False, outfilename="t_digest_figs",
                     location=in_tree, implementation=""):
    data = {}

    for prefix in prefixes:
        data[prefix] = {}
        filenames = filter(lambda x: x.startswith(prefix) and not x.endswith(cc_suffix),
                           os.listdir(location))
        for filename in filenames:
            value = filename.replace(prefix + "_", "").replace(".csv", "")
            with open(location + filename, 'r') as f:
                data[prefix][value] = pd.read_csv(f)

    centroid_data = {}
    centroid_counts = map(lambda x: x + cc_suffix, prefixes)
    for cc_name in centroid_counts:
        with open(location + cc_name, 'r') as f:
            centroid_data[cc_name.replace(cc_suffix, "")] = pd.read_csv(f)

    fig, ax = plt.subplots(len(prefixes), 3, squeeze=False)
    fig.set_figheight(4 * len(prefixes))
    fig.set_figwidth(15)

    for prefix in prefixes:
        error_q_list, norm_error_q_list = [], []
        pos = []
        for v in data[prefix]:
            pos.append(axis_labels[v])
            df = data[prefix][v]
            error_q_list.append(df['error_q'])
            norm_error_q_list.append(df['norm_error_q'])
        ax[prefixes.index(prefix), 0].set_title(clean_string(prefix) + implementation + " error")
        ax[prefixes.index(prefix), 0].boxplot(error_q_list, positions=pos, whis=[5, 95],
                                              showfliers=False)
        ax[prefixes.index(prefix), 1].set_title(
            clean_string(prefix) + implementation + " norm_error")
        ax[prefixes.index(prefix), 1].boxplot(norm_error_q_list, positions=pos, whis=[5, 95],
                                              showfliers=False)
        ax[prefixes.index(prefix), 2].set_title(
            clean_string(prefix) + implementation + " " + cc_suffix.replace(".csv", "").lstrip("_"))
        ax[prefixes.index(prefix), 2].hist(centroid_data[prefix]["centroid_count"], range=[5, 95],
                                           bins=30)

    fig.subplots_adjust(left=0.08, right=0.98, bottom=0.05, top=0.9,
                        hspace=0.4, wspace=0.3)

    if save is True:
        plt.savefig(outfilename)
    elif save is False:
        plt.show()


params = [(out_tree, in_tree, " (tree)"), (out_merging, in_merging, " (merging)")]


def main():
    for a, b, c in params:
        generate_figures(prefixes=["K_0_USUAL", "K_QUADRATIC"], save=True,
                         outfilename="{}t_digest_figs_K_0q".format(a), location=b, implementation=c)
        generate_figures(prefixes=["K_1_{}".format(y) for y in ["USUAL", "GLUED"]], save=True,
                         outfilename="{}t_digest_figs_K_1".format(a), location=b, implementation=c)
        generate_figures(prefixes=["K_2_{}".format(y) for y in ["USUAL", "GLUED"]], save=True,
                         outfilename="{}t_digest_figs_K_2".format(a), location=b, implementation=c)
        generate_figures(prefixes=["K_3_{}".format(y) for y in ["USUAL", "GLUED"]], save=True,
                         outfilename="{}t_digest_figs_K_3".format(a), location=b, implementation=c)


if __name__ == "__main__":
    main()
