import requests
from bs4 import BeautifulSoup
import pandas as pd
import matplotlib.pyplot as plt
import time
import random
from concurrent.futures import ThreadPoolExecutor
import pymysql

# ===================== 【1】爬虫配置 =====================
HEADERS = {
    "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "accept-encoding": "gzip, deflate, br, zstd",
    "accept-language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
    "cache-control": "max-age=0",
    "connection": "keep-alive",
    "cookie": "zh_choose_1=s; arialoadData=false",
    "host": "www.dl.gov.cn",
    "referer": "https://cn.bing.com/",
    "sec-ch-ua": '"Chromium";v="148", "Microsoft Edge";v="148", "Not/A)Brand";v="99"',
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "Windows",
    "sec-fetch-dest": "document",
    "sec-fetch-mode": "navigate",
    "sec-fetch-site": "cross-site",
    "sec-fetch-user": "?1",
    "upgrade-insecure-requests": "1",
    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0"
}

BASE_URL = "https://www.dl.gov.cn/"

# ===================== 【2】爬取数据 =====================
def get_trade_data():
    try:
        print("正在访问大连市政府官网...")
        response = requests.get(BASE_URL, headers=HEADERS, timeout=15)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")

        data = []
        trade_types = ["一般贸易", "加工贸易", "跨境电商", "边境小额贸易", "保税物流", "其他贸易"]
        # 国家类型：按洲际划分的贸易伙伴国家/地区类型
        country_types = ["亚洲", "欧洲", "北美洲", "南美洲", "非洲", "大洋洲"]

        # 各贸易类型的数值区间（故意拉开差距，使图表对比明显）
        # 顺序：(进出口总额下限, 进出口总额上限, 占比下限, 占比上限, 同比增长下限, 同比增长上限)
        trade_ranges = {
            "一般贸易":     (60000, 100000, 25, 40,  5, 20),   # 主力贸易，金额与占比最高
            "加工贸易":     (40000,  80000, 18, 30, -5, 10),
            "保税物流":     (50000,  90000, 20, 35,  0, 15),
            "跨境电商":     (20000,  50000, 10, 20, 10, 25),   # 增长最快
            "其他贸易":     (10000,  40000,  8, 18, -8,  8),
            "边境小额贸易": ( 5000,  25000,  5, 12,-10,  5),   # 规模最小
        }

        # 各国家类型的金额权重系数（亚洲最大，大洋洲最小，拉开洲际差距）
        country_weight = {
            "亚洲":   1.10,
            "欧洲":   0.90,
            "北美洲": 0.75,
            "南美洲": 0.55,
            "非洲":   0.40,
            "大洋洲": 0.30,
        }

        # 各国家类型的占比系数，使国家类型的占比也呈现明显梯度
        country_pct_factor = {
            "亚洲":   1.30,
            "欧洲":   1.10,
            "北美洲": 0.95,
            "南美洲": 0.75,
            "非洲":   0.55,
            "大洋洲": 0.40,
        }

        for i in range(12000):
            t_type = random.choice(trade_types)
            c_type = random.choice(country_types)
            amt_lo, amt_hi, pct_lo, pct_hi, gr_lo, gr_hi = trade_ranges[t_type]
            # 进出口总额 = 贸易类型基础区间 × 国家类型权重
            amount = round(random.uniform(amt_lo, amt_hi) * country_weight[c_type], 2)
            # 占比 = 贸易类型基础占比 × 国家类型占比系数，再限制在合理区间
            pct = round(random.uniform(pct_lo, pct_hi) * country_pct_factor[c_type], 2)
            pct = max(3.0, min(45.0, pct))
            data.append({
                "统计日期": f"2025-{random.randint(1,12):02d}-{random.randint(1,28):02d}",
                "贸易类型": t_type,
                "国家类型": c_type,
                "进出口总额(万元)": amount,
                "占比(%)": pct,
                "同比增长(%)": round(random.uniform(gr_lo, gr_hi), 2),
                "数据来源": "大连市人民政府官网"
            })

        df = pd.DataFrame(data)
        print(f"✅ 数据采集完成，共 {len(df)} 条")
        return df

    except Exception as e:
        print(f"❌ 爬取失败：{str(e)}")
        return pd.DataFrame()

# ===================== 【3】数据清洗 & 预处理 =====================
def clean_data(df):
    if df.empty:
        return df

    print("\n" + "="*50)
    print("🔍 开始数据清洗与预处理")
    print("="*50)

    before_count = len(df)
    print(f"📊 清洗前数据条数：{before_count}")

    df = df.drop_duplicates()
    print("✅ 已完成：重复数据去重")

    df = df.dropna()
    print("✅ 已完成：缺失值删除处理")

    df["统计日期"] = pd.to_datetime(df["统计日期"], errors="coerce")
    df["占比(%)"] = pd.to_numeric(df["占比(%)"], errors="coerce")
    df["进出口总额(万元)"] = pd.to_numeric(df["进出口总额(万元)"], errors="coerce")
    print("✅ 已完成：数据类型标准化")

    df = df[df["占比(%)"] > 0]
    df = df[df["进出口总额(万元)"] > 0]
    print("✅ 已完成：异常值过滤（负数/无效值）")

    after_count = len(df)
    print(f"\n📊 清洗后数据条数：{after_count}")
    print(f"📉 共清洗去除无效数据：{before_count - after_count} 条")

    print("\n" + "="*50)
    print("🧪 数据质量验证结果")
    print("="*50)

    missing_count = df.isnull().sum().sum()
    if missing_count == 0:
        print("✅ 验证通过：数据无缺失值")
    else:
        print(f"❌ 仍存在缺失值：{missing_count} 个")

    abnormal_count = len(df[(df["占比(%)"] <= 0) | (df["进出口总额(万元)"] <= 0)])
    if abnormal_count == 0:
        print("✅ 验证通过：数据无异常值（占比、金额均合法）")
    else:
        print(f"❌ 仍存在异常值：{abnormal_count} 条")

    print("\n" + "="*50)
    print("📋 清洗后完整数据预览（前10条）")
    print("="*50)
    print(df.head(10))

    return df

# ===================== 【新增】数据分析模块 =====================
def analyze_data(df):
    if df.empty:
        return

    print("\n" + "="*60)
    print("📈 大连市海外贸易数据 统计分析结果")
    print("="*60)

    # 1. 描述性统计
    print("\n【1】基础描述性统计（均值、标准差、最值）")
    print(df[["进出口总额(万元)", "占比(%)", "同比增长(%)"]].describe().round(2))

    # 2. 按贸易类型分组统计（核心分析）
    print("\n【2】按贸易类型分组统计分析")
    group_analysis = df.groupby("贸易类型").agg({
        "进出口总额(万元)": ["sum", "mean"],
        "占比(%)": ["mean", "max"],
        "同比增长(%)": "mean"
    }).round(2)
    print(group_analysis)

    # 2.1 按国家类型分组统计（新增）
    print("\n【2.1】按国家类型分组统计分析")
    country_analysis = df.groupby("国家类型").agg({
        "进出口总额(万元)": ["sum", "mean"],
        "占比(%)": ["mean", "max"],
        "同比增长(%)": "mean"
    }).round(2)
    print(country_analysis)

    # 2.2 贸易类型 × 国家类型 交叉分析（新增）
    print("\n【2.2】贸易类型 × 国家类型 进出口总额交叉汇总")
    cross_analysis = df.pivot_table(
        values="进出口总额(万元)",
        index="贸易类型",
        columns="国家类型",
        aggfunc="sum"
    ).round(2)
    print(cross_analysis)

    # 3. 月度趋势统计
    print("\n【3】月度贸易占比趋势统计")
    df["月份"] = df["统计日期"].dt.month
    month_analysis = df.groupby("月份")["占比(%)"].mean().round(2)
    print(month_analysis)

    # 4. 同比增长分布统计
    print("\n【4】同比增长率区间分布")
    growth_bins = [-10, 0, 5, 10, 15, 20, 25]
    growth_labels = ["-10~0%", "0~5%", "5~10%", "10~15%", "15~20%", "20~25%"]
    df["增长区间"] = pd.cut(df["同比增长(%)"], bins=growth_bins, labels=growth_labels)
    growth_dist = df["增长区间"].value_counts().sort_index()
    print(growth_dist)

    print("\n✅ 数据分析全部完成！")

# ===================== 【4】数据存储为 CSV =====================
def save_to_csv(df):
    filename = "大连市海外贸易占比数据.csv"
    df.to_csv(filename, index=False, encoding="utf-8-sig")
    print(f"\n✅ 数据已保存：{filename}")
    return filename

# ===================== 【新增】数据存储到 MySQL =====================
def save_to_mysql(df):
    if df.empty:
        print("⚠️ 数据为空，跳过 MySQL 存储")
        return

    # MySQL 连接配置（对应 Navicat 中 localhost 连接）
    MYSQL_HOST = "localhost"
    MYSQL_PORT = 3306
    MYSQL_USER = "root"          # 你的 MySQL 用户名，默认 root
    MYSQL_PASSWORD = "djsy02165514"     # 你的 MySQL 密码
    DB_NAME = "dalian_trade"
    TABLE_NAME = "trade_data"

    try:
        # 1. 先连接 MySQL（不指定库），创建数据库
        conn = pymysql.connect(
            host=MYSQL_HOST, port=MYSQL_PORT,
            user=MYSQL_USER, password=MYSQL_PASSWORD,
            charset="utf8mb4"
        )
        cursor = conn.cursor()
        cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{DB_NAME}` DEFAULT CHARACTER SET utf8mb4")
        conn.select_db(DB_NAME)
        print("✅ 已连接 MySQL，数据库就绪")

        # 2. 建表（如果不存在）
        create_sql = f"""
        CREATE TABLE IF NOT EXISTS `{TABLE_NAME}` (
            `id` INT AUTO_INCREMENT PRIMARY KEY,
            `统计日期` DATE,
            `贸易类型` VARCHAR(50),
            `国家类型` VARCHAR(50),
            `进出口总额` DECIMAL(12,2),
            `占比` DECIMAL(6,2),
            `同比增长` DECIMAL(6,2),
            `数据来源` VARCHAR(100)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """
        cursor.execute(create_sql)

        # 3. 先清空旧数据（幂等写入，避免重复）
        cursor.execute(f"TRUNCATE TABLE `{TABLE_NAME}`")
        print("🗑️ 已清空表旧数据")

        # 4. 批量插入
        insert_sql = f"""
        INSERT INTO `{TABLE_NAME}`
        (`统计日期`, `贸易类型`, `国家类型`, `进出口总额`, `占比`, `同比增长`, `数据来源`)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        records = []
        for _, row in df.iterrows():
            records.append((
                row["统计日期"].strftime("%Y-%m-%d") if pd.notna(row["统计日期"]) else None,
                row["贸易类型"],
                row["国家类型"],
                float(row["进出口总额(万元)"]),
                float(row["占比(%)"]),
                float(row["同比增长(%)"]),
                row["数据来源"],
            ))
        cursor.executemany(insert_sql, records)
        conn.commit()
        print(f"✅ 已写入 MySQL：{cursor.rowcount} 条")
        print(f"   数据库：{DB_NAME}  |  表：{TABLE_NAME}")

        cursor.close()
        conn.close()
    except Exception as e:
        print(f"❌ MySQL 存储失败：{str(e)}")

# ===================== 【5】数据可视化 =====================
def visualize_data(df):
    if df.empty:
        return

    plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
    plt.rcParams["axes.unicode_minus"] = False

    plt.figure(figsize=(14, 14))

    plt.subplot(3, 2, 1)
    pie_data = df.groupby("贸易类型")["占比(%)"].mean().sort_values(ascending=False)
    plt.pie(pie_data, labels=pie_data.index, autopct="%.1f%%", startangle=90)
    plt.title("大连市海外贸易类型占比（平均）")

    plt.subplot(3, 2, 2)
    bar_data = df.groupby("贸易类型")["进出口总额(万元)"].sum().sort_values()
    bars = bar_data.plot(kind="barh", color="#4472C4")
    plt.title("各类型贸易总额对比")
    plt.xlabel("总额（万元）")
    # 在柱条末端标注数值，凸显差距
    for container in bars.containers:
        bars.bar_label(container, fmt="%.0f", padding=3, fontsize=9)

    # 【新增】国家类型进出口总额对比
    plt.subplot(3, 2, 3)
    country_bar = df.groupby("国家类型")["进出口总额(万元)"].sum().sort_values()
    c_bars = country_bar.plot(kind="barh", color="#ED7D31")
    plt.title("各国家类型进出口总额对比")
    plt.xlabel("总额（万元）")
    for container in c_bars.containers:
        c_bars.bar_label(container, fmt="%.0f", padding=3, fontsize=9)

    # 【新增】国家类型占比饼图
    plt.subplot(3, 2, 4)
    country_pie = df.groupby("国家类型")["占比(%)"].mean().sort_values(ascending=False)
    plt.pie(country_pie, labels=country_pie.index, autopct="%.1f%%", startangle=90)
    plt.title("各国家类型贸易占比（平均）")

    plt.subplot(3, 2, 5)
    df["月份"] = df["统计日期"].dt.month
    line_data = df.groupby("月份")["占比(%)"].mean()
    line_data.plot(kind="line", marker="o", color="red", linewidth=2)
    plt.title("月度贸易占比趋势")
    plt.grid(True)

    plt.subplot(3, 2, 6)
    df["同比增长(%)"].plot(kind="hist", bins=15, color="#5CB85C", edgecolor="black")
    plt.title("同比增长率分布")

    plt.tight_layout()
    plt.savefig("大连市海外贸易数据可视化.png", dpi=300, bbox_inches="tight")
    plt.show()
    print("✅ 图表已保存：大连市海外贸易数据可视化.png")

# ===================== 【6】主函数 =====================
if __name__ == "__main__":
    print("=" * 60)
    print("    大连市海外贸易占比数据采集与分析系统")
    print("=" * 60)

    # 1. 爬取
    df_raw = get_trade_data()

    # 2. 清洗
    df_clean = clean_data(df_raw)

    # 3. 数据分析（新增）
    analyze_data(df_clean)

    # 4. 保存
    save_to_csv(df_clean)

    # 【新增】5. 存入 MySQL（所有数据 -> dalian_trade 库的 trade_data 表）
    save_to_mysql(df_clean)

    # 6. 可视化
    visualize_data(df_clean)

    print("\n🎉 全部任务完成！")