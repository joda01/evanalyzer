import json
import urllib.request



# 


urllib.request.urlretrieve("https://sites.imagej.net/stats.json", "stats.json")


nrTotal = 0
with open('stats.json') as f:
    stats = json.load(f)
    ev = stats["evanalyzer"]
    for date, nr in ev.items():
        print(date + " " + str(nr))
        nrTotal = nrTotal + nr

print("Total: " + str(nrTotal))


#recent_totals = {
#    site: sum(v for date, v in counts.items() if date.startswith(f'{year}-'))
#    if isinstance(counts, dict) else 0
#    for site, counts in stats.items()
#}
#
#popular = dict(sorted(recent_totals.items(), key=lambda x:x[1]))
#
#for site, total in popular.items():
#    if total > cutoff:
#        print(f"* {site} = {total}")
