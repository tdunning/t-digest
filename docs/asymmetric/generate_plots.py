import os
import pandas as pd
import matplotlib.pyplot as plt

in_prefix = "data"
out_prefix = "plots"

implementations = ["tree", "merging"]
distributions = ["UNIFORM", "EXPONENTIAL"]

scale_function_prefixes = ["K_{0}_{1}".format(x, y) for x in ["1", "2", "3"] for y in
                           ["USUAL", "GLUED"]] + ["K_0_USUAL"] + ["K_QUADRATIC"]


def clean_string(s):
    return s.replace("K", "k").replace("_USUAL", "").replace("GLUED", "glued"). \
        replace("QUADRATIC", "quadratic")


cc_suffix = "_centroid_counts.csv"
cs_suffix = "_centroid_sizes.csv"

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


def generate_figures(prefixes=scale_function_prefixes, save=False, outfilename="",
                     location="", implementation=""):
    data = {}

    for prefix in prefixes:
        data[prefix] = {}
        filenames = filter(
            lambda x: x.startswith(prefix) and not x.endswith(cc_suffix) and not x.endswith(
                cs_suffix),
            os.listdir(location))
        for filename in filenames:
            value = filename.replace(prefix + "_", "").replace(".csv", "")
            with open(location + filename, 'r') as f:
                data[prefix][value] = pd.read_csv(f)

    centroid_count_data = {}
    centroid_counts = map(lambda x: x + cc_suffix, prefixes)
    for cc_name in centroid_counts:
        with open(location + cc_name, 'r') as f:
            centroid_count_data[cc_name.replace(cc_suffix, "")] = pd.read_csv(f)

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
        ax[prefixes.index(prefix), 0].set_yscale('log')
        ax[prefixes.index(prefix), 1].set_title(
            clean_string(prefix) + implementation + " norm_error")
        ax[prefixes.index(prefix), 1].boxplot(norm_error_q_list, positions=pos, whis=[5, 95],
                                              showfliers=False)
        ax[prefixes.index(prefix), 1].set_yscale('log')
        ax[prefixes.index(prefix), 2].set_title(
            clean_string(prefix) + implementation + " " + cc_suffix.replace(".csv", "").lstrip("_"))
        ax[prefixes.index(prefix), 2].hist(centroid_count_data[prefix]["centroid_count"],
                                           range=[5, 95],
                                           bins=30)

    fig.subplots_adjust(left=0.08, right=0.98, bottom=0.05, top=0.9,
                        hspace=0.4, wspace=0.3)

    if save is True:
        plt.savefig(outfilename)
    elif save is False:
        plt.show()


def generate_size_figures(prefix="K_0_USUAL", save=False, outfilename="", value='0.01',
                          location="", centroid_index=0):
    data = {}
    centroid_sizes_data = {}

    for impl in implementations:
        data[impl] = {}
        centroid_sizes_data[impl] = {}
        for dist in distributions:
            data[impl][dist] = {}
            centroid_sizes_data[impl][dist] = {}
            filename = "{0}_{1}.csv".format(prefix, value)
            with open("{0}/{1}/{2}".format(location, impl, dist) + "/" + filename, 'r') as f:
                data[impl][dist][value] = pd.read_csv(f)
            with open("{0}/{1}/{2}".format(location, impl, dist) + "/" + prefix + cs_suffix,
                      'r') as f:
                _d = f.readlines()
                centroid_sizes_data[impl][dist][prefix] = [
                    [int(x) for x in y.rstrip(',\n').split(',')] for y in _d]

    fig, ax = plt.subplots(len(implementations), len(distributions), squeeze=False)
    fig.set_figheight(15)
    fig.set_figwidth(15)

    for impl in implementations:
        for dist in distributions:
            error_q_list, norm_error_q_list = [], []
            pos = []
            for v in data[impl][dist]:
                pos.append(axis_labels[v])
                df = data[impl][dist][v]
                error_q_list.append(df['error_q'])
                norm_error_q_list.append(df['norm_error_q'])
                title = "{0}, {1}, {2}, q={3}, index {4}".format(clean_string(prefix), impl,
                                                                 dist.lower(), value,
                                                                 str(centroid_index))
                ax[implementations.index(impl), distributions.index(dist)].set_title(title)
                _a, b = centroid_sizes_data[impl][dist][prefix], df['norm_error_q']
                a = [i[centroid_index] for i in _a]
                ax[implementations.index(impl), distributions.index(dist)].scatter(a, b)

    fig.subplots_adjust(left=0.08, right=0.98, bottom=0.05, top=0.9,
                        hspace=0.4, wspace=0.3)

    if save is True:
        plt.savefig(outfilename)
    elif save is False:
        plt.show()


def generate_figures_both_distr(prefixes=scale_function_prefixes, save=False, outfilename="",
                                locations=[""], implementation=""):
    data = {}

    for prefix in prefixes:
        data[prefix] = {}
        for location in locations:
            data[prefix][location] = {}
            filenames = filter(
                lambda x: x.startswith(prefix) and not x.endswith(cc_suffix) and not x.endswith(
                    cs_suffix),
                os.listdir(location))
            for filename in filenames:
                value = filename.replace(prefix + "_", "").replace(".csv", "")
                with open(location + filename, 'r') as f:
                    data[prefix][location][value] = pd.read_csv(f)

    centroid_count_data = {}
    centroid_counts = map(lambda x: x + cc_suffix, prefixes)
    for cc_name in centroid_counts:
        centroid_count_data[cc_name.replace(cc_suffix, "")] = {}
        for location in locations:
            # centroid_count_data[cc_name.replace(cc_suffix, "")][location] = {}
            with open(location + cc_name, 'r') as f:
                centroid_count_data[cc_name.replace(cc_suffix, "")][location] = pd.read_csv(f)

    fig, ax = plt.subplots(len(prefixes), 3, squeeze=False)
    fig.set_figheight(4 * len(prefixes))
    fig.set_figwidth(15)

    for prefix in prefixes:
        error_q_list, norm_error_q_list = {}, {}
        pos = {}
        for location in locations:
            error_q_list[location] = []
            norm_error_q_list[location] = []
            pos[location] = []
            for v in data[prefix][location]:
                pos[location].append(axis_labels[v])
                df = data[prefix][location][v]
                error_q_list[location].append(df['error_q'])
                norm_error_q_list[location].append(df['norm_error_q'])

        location_0, location_1 = locations

        ax[prefixes.index(prefix), 0].set_title(clean_string(prefix) + implementation + " error")
        ax[prefixes.index(prefix), 0].boxplot(error_q_list[location_0],
                                              positions=[x - .15 for x in pos[location_0]],
                                              whis=[5, 95],
                                              showfliers=False, widths=0.2,
                                              medianprops=dict(linestyle='-', linewidth=1.5,
                                                               label=location_0.split('/')[
                                                                   -2].lower(), color='orange'))
        ax[prefixes.index(prefix), 0].boxplot(error_q_list[location_1],
                                              positions=[x + .15 for x in pos[location_1]],
                                              whis=[5, 95],
                                              showfliers=False, widths=0.2,
                                              medianprops=dict(linestyle='-', linewidth=4.5,
                                                               label=location_1.split('/')[
                                                                   -2].lower(), color='blue'))

        handles, labels = ax[prefixes.index(prefix), 0].get_legend_handles_labels()
        ax[prefixes.index(prefix), 0].legend([handles[0], handles[-1]], [labels[0], labels[-1]])
        ax[prefixes.index(prefix), 0].set_xticks(range(-5, 6))
        ax[prefixes.index(prefix), 0].set_xticklabels(range(-5, 6))
        ax[prefixes.index(prefix), 0].set_yscale('log')

        ax[prefixes.index(prefix), 1].set_title(
            clean_string(prefix) + implementation + " norm_error")
        ax[prefixes.index(prefix), 1].boxplot(norm_error_q_list[location_0],
                                              positions=[x - .15 for x in pos[location_0]],
                                              whis=[5, 95],
                                              showfliers=False, widths=0.2,
                                              medianprops=dict(linewidth=1.5,
                                                               label=location_0.split('/')[
                                                                   -2].lower(), color='orange'))
        ax[prefixes.index(prefix), 1].boxplot(norm_error_q_list[location_1],
                                              positions=[x + .15 for x in pos[location_1]],
                                              whis=[5, 95],
                                              showfliers=False, widths=0.2,
                                              medianprops=dict(linewidth=4.5,
                                                               label=location_1.split('/')[
                                                                   -2].lower(), color='blue'))

        handles, labels = ax[prefixes.index(prefix), 1].get_legend_handles_labels()
        ax[prefixes.index(prefix), 1].legend([handles[0], handles[-1]], [labels[0], labels[-1]])
        ax[prefixes.index(prefix), 1].set_xticks(range(-5, 6))
        ax[prefixes.index(prefix), 1].set_xticklabels(range(-5, 6))
        ax[prefixes.index(prefix), 1].set_yscale('log')

        ax[prefixes.index(prefix), 2].set_title(
            clean_string(prefix) + implementation + " " + cc_suffix.replace(".csv", "").lstrip("_"))
        ax[prefixes.index(prefix), 2].hist(
            centroid_count_data[prefix][location_0]["centroid_count"], range=[20, 100],
            bins=40, color='orange', alpha=0.5, label=location_0.split('/')[-2].lower())
        ax[prefixes.index(prefix), 2].hist(
            centroid_count_data[prefix][location_1]["centroid_count"], range=[20, 100],
            bins=40, color='blue', alpha=0.5, label=location_1.split('/')[-2].lower())
        ax[prefixes.index(prefix), 2].legend()

    fig.subplots_adjust(left=0.08, right=0.98, bottom=0.05, top=0.9,
                        hspace=0.4, wspace=0.3)
    if save is True:
        plt.savefig(outfilename)
    elif save is False:
        plt.show()
    plt.show()


# for separate plots for the two distributions
_params = [
    ("{0}/{1}/{2}/".format(out_prefix, impl, dist), "{0}/{1}/{2}/".format(in_prefix, impl, dist),
     " ({0}, {1})".format(impl, dist.lower())) for impl in implementations for dist in
    distributions]

params = [("{0}/{1}/BOTH/".format(out_prefix, impl),
           ["{0}/{1}/{2}/".format(in_prefix, impl, dist) for dist in distributions],
           " ({0})".format(impl)) for impl in ['tree', 'merging']]


def main():
    for a, b, c in params:
        generate_figures_both_distr(prefixes=["K_0_USUAL", "K_QUADRATIC"], save=True,
                                    outfilename="{}t_digest_figs_K_0q".format(a), locations=b,
                                    implementation=c)
        generate_figures_both_distr(prefixes=["K_1_{}".format(y) for y in ["USUAL", "GLUED"]],
                                    save=True,
                                    outfilename="{}t_digest_figs_K_1".format(a), locations=b,
                                    implementation=c)
        generate_figures_both_distr(prefixes=["K_2_{}".format(y) for y in ["USUAL", "GLUED"]],
                                    save=True,
                                    outfilename="{}t_digest_figs_K_2".format(a), locations=b,
                                    implementation=c)
        generate_figures_both_distr(prefixes=["K_3_{}".format(y) for y in ["USUAL", "GLUED"]],
                                    save=True,
                                    outfilename="{}t_digest_figs_K_3".format(a), locations=b,
                                    implementation=c)
    for v in ['0.99', '0.999']:
        fcn = 'K_0_USUAL'
        centroid_index = -1
        outfile = out_prefix + '/' + 'size/' + fcn + '_' + v + '_' + str(centroid_index) + '.png'
        generate_size_figures(location=in_prefix + '/', prefix=fcn, value=v,
                              centroid_index=centroid_index,
                              outfilename=outfile, save=True)
        generate_size_figures(location=in_prefix + '/', prefix=fcn, value=v,
                              centroid_index=centroid_index,
                              outfilename=outfile, save=True)

    # these plots are no longer used in the paper
    for a, b, c in _params:
        generate_figures(prefixes=["K_0_USUAL", "K_QUADRATIC"], save=True,
                         outfilename="{}t_digest_figs_K_0q".format(a), location=b,
                         implementation=c)
        generate_figures(prefixes=["K_1_{}".format(y) for y in ["USUAL", "GLUED"]], save=True,
                         outfilename="{}t_digest_figs_K_1".format(a), location=b,
                         implementation=c)
        generate_figures(prefixes=["K_2_{}".format(y) for y in ["USUAL", "GLUED"]], save=True,
                         outfilename="{}t_digest_figs_K_2".format(a), location=b,
                         implementation=c)
        generate_figures(prefixes=["K_3_{}".format(y) for y in ["USUAL", "GLUED"]], save=True,
                         outfilename="{}t_digest_figs_K_3".format(a), location=b,
                         implementation=c)


if __name__ == "__main__":
    main()
