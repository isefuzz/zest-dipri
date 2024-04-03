import matplotlib
import matplotlib.pyplot as plt

# To avoid type-3 font error
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
plt.rcParams['font.size'] = 16

fuzzers = [
    # Baselines
    'zest',
    # For ablation experiment
    'dipri-AE', 'dipri-AH', 'dipri-PE',
    'dipri-PH', 'dipri-VE',  'dipri-VH'
]

targets = [
    'bcel', 'ant','rhino','closure','chess'
]

outs = [
    'out-0', 'out-1','out-2','out-3', 'out-4','out-5','out-6', 'out-7','out-8','out-9'
]
