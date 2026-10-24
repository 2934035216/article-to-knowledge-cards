"""
大连市海外贸易数据采集与可视化系统
五大模块架构（与系统流程图一致）：
  1. 用户管理模块：用户注册 / 用户登录 / 个人信息管理 / 用户账号管理
  2. 贸易信息管理模块：贸易信息管理 / 贸易分类管理 / 贸易详情管理 / 贸易信息维护
  3. 贸易查询模块：贸易搜索 / 地区筛选 / 类型筛选 / 占比筛选 / 详情查看
  4. 数据分析与可视化模块：占比分析 / 增长分析 / 金额分析 / 热度分析 / 地区分布 / 类型分析
  5. 系统管理模块：公告管理 / 数据统计 / 数据维护 / 系统信息管理
后端：Python 标准库 http.server（零依赖） + pymysql
启动：python app.py -> http://localhost:5000
默认管理员：admin / admin123
"""
import hashlib
import json
import os
import time
import uuid
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

import pandas as pd
import pymysql

# ===================== 配置 =====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CSV_PATH = os.path.join(BASE_DIR, "大连市海外贸易占比数据.csv")
MYSQL_HOST = "localhost"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASSWORD = "djsy02165514"
DB_NAME = "dalian_trade"
TABLE_NAME = "trade_data"
PORT = 5000
START_TIME = time.time()
APP_VERSION = "v2.0"

SESSIONS = {}  # token -> {username, role, nickname}
SALT = "dalian_trade_salt"


def hash_pwd(p):
    return hashlib.sha256((SALT + str(p)).encode("utf-8")).hexdigest()


def db_connect(with_db=True):
    return pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT, user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=DB_NAME if with_db else None,
        charset="utf8mb4",
    )


# ===================== 数据库初始化 =====================
def init_db():
    """创建业务库、用户表、公告表，并内置管理员账号"""
    conn = db_connect(with_db=False)
    cur = conn.cursor()
    cur.execute(f"CREATE DATABASE IF NOT EXISTS `{DB_NAME}` DEFAULT CHARACTER SET utf8mb4")
    conn.select_db(DB_NAME)
    cur.execute("""CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(50) UNIQUE NOT NULL,
        password VARCHAR(64) NOT NULL,
        nickname VARCHAR(50) DEFAULT '',
        email VARCHAR(100) DEFAULT '',
        role VARCHAR(20) DEFAULT 'user',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""")
    cur.execute("""CREATE TABLE IF NOT EXISTS announcements (
        id INT AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(200) NOT NULL,
        content TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""")
    # 公告表扩展字段（与公告编辑页面一致）：类别/首页图片/摘要/点击率/发布人
    new_cols = [
        ("category", "VARCHAR(50) DEFAULT '通知'"),
        ("image", "VARCHAR(500) DEFAULT ''"),
        ("summary", "VARCHAR(500) DEFAULT ''"),
        ("clicks", "INT DEFAULT 0"),
        ("publisher", "VARCHAR(50) DEFAULT ''"),
    ]
    for col, spec in new_cols:
        cur.execute(
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA=%s AND TABLE_NAME='announcements' AND COLUMN_NAME=%s",
            (DB_NAME, col))
        if cur.fetchone()[0] == 0:
            cur.execute(f"ALTER TABLE announcements ADD COLUMN {col} {spec}")
    cur.execute(
        "INSERT IGNORE INTO users (username,password,nickname,role) VALUES (%s,%s,%s,%s)",
        ("admin", hash_pwd("admin123"), "系统管理员", "admin"),
    )
    conn.commit()
    cur.close()
    conn.close()
    print("[初始化] 数据库/用户表/公告表就绪，内置管理员 admin/admin123")


# ===================== 贸易数据加载 =====================
def load_data():
    """优先从 MySQL 读取，失败则回退 CSV"""
    try:
        conn = db_connect()
        df = pd.read_sql(f"SELECT * FROM `{TABLE_NAME}`", conn)
        conn.close()
        if not df.empty:
            df = df.rename(columns={
                "进出口总额": "进出口总额(万元)",
                "占比": "占比(%)",
                "同比增长": "同比增长(%)",
            })
            if "id" in df.columns:
                df = df.rename(columns={"id": "record_id"})
            print(f"[数据源] MySQL 读取成功，共 {len(df)} 条")
            return _normalize(df)
    except Exception as e:
        print(f"[数据源] MySQL 不可用，回退 CSV：{e}")

    if os.path.exists(CSV_PATH):
        df = pd.read_csv(CSV_PATH)
        df.insert(0, "record_id", range(1, len(df) + 1))
        print(f"[数据源] CSV 读取成功，共 {len(df)} 条")
        return _normalize(df)

    print("[数据源] 无可用数据")
    return pd.DataFrame()


def _normalize(df):
    if df.empty:
        return df
    df["统计日期"] = pd.to_datetime(df["统计日期"], errors="coerce")
    for col in ["进出口总额(万元)", "占比(%)", "同比增长(%)"]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")
    if "月份" not in df.columns:
        df["月份"] = df["统计日期"].dt.month
    return df


def reload_data():
    global DF
    DF = load_data()


def import_csv_to_mysql():
    """数据维护：把 CSV 全量导入 MySQL（先清空）"""
    df = pd.read_csv(CSV_PATH)
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(f"""CREATE TABLE IF NOT EXISTS `{TABLE_NAME}` (
        id INT AUTO_INCREMENT PRIMARY KEY,
        统计日期 DATE, 贸易类型 VARCHAR(50), 国家类型 VARCHAR(50),
        进出口总额 DECIMAL(12,2), 占比 DECIMAL(6,2), 同比增长 DECIMAL(6,2),
        数据来源 VARCHAR(100)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""")
    cur.execute(f"TRUNCATE TABLE `{TABLE_NAME}`")
    sql = (f"INSERT INTO `{TABLE_NAME}` (统计日期,贸易类型,国家类型,进出口总额,占比,同比增长,数据来源) "
           "VALUES (%s,%s,%s,%s,%s,%s,%s)")
    records = [
        (pd.to_datetime(r["统计日期"]).strftime("%Y-%m-%d") if pd.notna(r["统计日期"]) else None,
         r["贸易类型"], r["国家类型"],
         float(r["进出口总额(万元)"]), float(r["占比(%)"]), float(r["同比增长(%)"]),
         r["数据来源"])
        for _, r in df.iterrows()
    ]
    cur.executemany(sql, records)
    conn.commit()
    n = cur.rowcount
    cur.close()
    conn.close()
    reload_data()
    return n


init_db()
DF = load_data()


# ===================== 记录序列化 =====================
def row_to_dict(row, rid):
    return {
        "id": int(rid),
        "统计日期": row["统计日期"].strftime("%Y-%m-%d") if pd.notna(row["统计日期"]) else "",
        "贸易类型": str(row["贸易类型"]),
        "国家类型": str(row["国家类型"]),
        "进出口总额": round(float(row["进出口总额(万元)"]), 2),
        "占比": round(float(row["占比(%)"]), 2),
        "同比增长": round(float(row["同比增长(%)"]), 2),
        "数据来源": str(row["数据来源"]),
    }


# ================================================================
# 模块一：用户管理（注册 / 登录 / 个人信息 / 账号管理）
# ================================================================
def api_register(q, body, user):
    username = (body.get("username") or "").strip()
    password = body.get("password") or ""
    if not username or not password:
        return {"ok": False, "msg": "用户名和密码不能为空"}, 400
    conn = db_connect()
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO users (username,password,nickname,email,role) VALUES (%s,%s,%s,%s,%s)",
            (username, hash_pwd(password), body.get("nickname") or username,
             body.get("email") or "", "user"),
        )
        conn.commit()
        return {"ok": True, "msg": "注册成功，请登录"}
    except pymysql.err.IntegrityError:
        return {"ok": False, "msg": "用户名已存在"}, 400
    finally:
        cur.close()
        conn.close()


def api_login(q, body, user):
    username = (body.get("username") or "").strip()
    password = body.get("password") or ""
    conn = db_connect()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT * FROM users WHERE username=%s AND password=%s",
                (username, hash_pwd(password)))
    row = cur.fetchone()
    cur.close()
    conn.close()
    if not row:
        return {"ok": False, "msg": "用户名或密码错误"}, 401
    token = uuid.uuid4().hex
    SESSIONS[token] = {"username": row["username"], "role": row["role"],
                       "nickname": row["nickname"]}
    return {"ok": True, "token": token, "role": row["role"],
            "nickname": row["nickname"], "username": row["username"]}


def api_logout(q, body, user):
    token = body.get("_token")
    SESSIONS.pop(token, None)
    return {"ok": True, "msg": "已退出登录"}


def api_profile(q, body, user):
    conn = db_connect()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT username,nickname,email,role,created_at FROM users WHERE username=%s",
                (user["username"],))
    row = cur.fetchone()
    cur.close()
    conn.close()
    return {"profile": row}


def api_profile_update(q, body, user):
    nickname = body.get("nickname") or user["nickname"]
    email = body.get("email") or ""
    conn = db_connect()
    cur = conn.cursor()
    cur.execute("UPDATE users SET nickname=%s, email=%s WHERE username=%s",
                (nickname, email, user["username"]))
    if body.get("password"):
        cur.execute("UPDATE users SET password=%s WHERE username=%s",
                    (hash_pwd(body["password"]), user["username"]))
    conn.commit()
    cur.close()
    conn.close()
    SESSIONS.update({body["_token"]: {**user, "nickname": nickname}})
    return {"ok": True, "msg": "个人信息已更新"}


def api_users(q, body, user):
    conn = db_connect()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT id,username,nickname,email,role,created_at FROM users ORDER BY id")
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return {"users": rows}


def api_users_add(q, body, user):
    username = (body.get("username") or "").strip()
    password = body.get("password") or ""
    if not username or not password:
        return {"ok": False, "msg": "用户名和密码不能为空"}, 400
    conn = db_connect()
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO users (username,password,nickname,email,role) VALUES (%s,%s,%s,%s,%s)",
            (username, hash_pwd(password), body.get("nickname") or username,
             body.get("email") or "", body.get("role") or "user"))
        conn.commit()
        return {"ok": True, "msg": "用户已添加"}
    except pymysql.err.IntegrityError:
        return {"ok": False, "msg": "用户名已存在"}, 400
    finally:
        cur.close()
        conn.close()


def api_users_delete(q, body, user):
    uid = q.get("id", [None])[0]
    conn = db_connect()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT username,role FROM users WHERE id=%s", (uid,))
    row = cur.fetchone()
    if not row:
        cur.close(); conn.close()
        return {"ok": False, "msg": "用户不存在"}, 404
    if row["username"] == "admin":
        cur.close(); conn.close()
        return {"ok": False, "msg": "不能删除内置管理员"}, 400
    cur.execute("DELETE FROM users WHERE id=%s", (uid,))
    conn.commit()
    cur.close()
    conn.close()
    return {"ok": True, "msg": f"已删除用户 {row['username']}"}


# ================================================================
# 模块二：贸易信息管理（信息管理 / 分类管理 / 详情管理 / 信息维护）
# ================================================================
def api_records(q, body, user):
    """分页 + 多条件筛选"""
    if DF.empty:
        return {"total": 0, "page": 1, "size": 15, "records": []}
    page = max(1, int(q.get("page", ["1"])[0]))
    size = max(1, min(int(q.get("size", ["15"])[0]), 200))
    df = DF
    kw = q.get("q", [""])[0].strip()
    if kw:
        mask = (df["贸易类型"].astype(str).str.contains(kw)
                | df["国家类型"].astype(str).str.contains(kw)
                | df["数据来源"].astype(str).str.contains(kw))
        df = df[mask]
    country = q.get("country", [""])[0].strip()
    if country:
        df = df[df["国家类型"] == country]
    ttype = q.get("type", [""])[0].strip()
    if ttype:
        df = df[df["贸易类型"] == ttype]
    if q.get("min_pct", [""])[0]:
        df = df[df["占比(%)"] >= float(q["min_pct"][0])]
    if q.get("max_pct", [""])[0]:
        df = df[df["占比(%)"] <= float(q["max_pct"][0])]
    if q.get("min_growth", [""])[0]:
        df = df[df["同比增长(%)"] >= float(q["min_growth"][0])]
    if q.get("max_growth", [""])[0]:
        df = df[df["同比增长(%)"] <= float(q["max_growth"][0])]
    total = len(df)
    start = (page - 1) * size
    page_df = df.iloc[start:start + size]
    records = [row_to_dict(row, row["record_id"]) for _, row in page_df.iterrows()]
    return {"total": int(total), "page": page, "size": size, "records": records}


def api_record_detail(q, body, user):
    rid = q.get("id", [None])[0]
    row = DF[DF["record_id"] == int(rid)]
    if row.empty:
        return {"error": "记录不存在"}, 404
    return {"record": row_to_dict(row.iloc[0], rid)}


def _validate_record(body):
    try:
        return {
            "统计日期": pd.to_datetime(body["统计日期"]).strftime("%Y-%m-%d"),
            "贸易类型": str(body["贸易类型"]).strip(),
            "国家类型": str(body["国家类型"]).strip(),
            "进出口总额": float(body["进出口总额"]),
            "占比": float(body["占比"]),
            "同比增长": float(body["同比增长"]),
            "数据来源": str(body.get("数据来源") or "手工录入"),
        }
    except (KeyError, ValueError):
        return None


def api_records_add(q, body, user):
    r = _validate_record(body)
    if not r:
        return {"ok": False, "msg": "字段不完整或格式错误"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(
        f"INSERT INTO `{TABLE_NAME}` (统计日期,贸易类型,国家类型,进出口总额,占比,同比增长,数据来源) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s)",
        (r["统计日期"], r["贸易类型"], r["国家类型"], r["进出口总额"],
         r["占比"], r["同比增长"], r["数据来源"]))
    conn.commit()
    cur.close()
    conn.close()
    reload_data()
    return {"ok": True, "msg": "新增成功"}


def api_records_update(q, body, user):
    rid = body.get("id")
    r = _validate_record(body)
    if not r or not rid:
        return {"ok": False, "msg": "字段不完整"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(
        f"UPDATE `{TABLE_NAME}` SET 统计日期=%s,贸易类型=%s,国家类型=%s,进出口总额=%s,"
        "占比=%s,同比增长=%s,数据来源=%s WHERE id=%s",
        (r["统计日期"], r["贸易类型"], r["国家类型"], r["进出口总额"],
         r["占比"], r["同比增长"], r["数据来源"], rid))
    conn.commit()
    n = cur.rowcount
    cur.close()
    conn.close()
    reload_data()
    return {"ok": True, "msg": f"已更新（{n} 条）"}


def api_records_delete(q, body, user):
    rid = body.get("id") or (q.get("id", [None])[0])
    if not rid:
        return {"ok": False, "msg": "缺少 id"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(f"DELETE FROM `{TABLE_NAME}` WHERE id=%s", (rid,))
    conn.commit()
    n = cur.rowcount
    cur.close()
    conn.close()
    reload_data()
    return {"ok": True, "msg": f"已删除（{n} 条）"}


FIELD_MAP = {"trade": "贸易类型", "country": "国家类型"}


def api_categories(q, body, user):
    if DF.empty:
        return {"types": [], "countries": []}
    t = DF.groupby("贸易类型").size()
    c = DF.groupby("国家类型").size()
    return {
        "types": [{"name": k, "count": int(v)} for k, v in t.items()],
        "countries": [{"name": k, "count": int(v)} for k, v in c.items()],
    }


def api_category_rename(q, body, user):
    field = FIELD_MAP.get(body.get("field"))
    old, new = (body.get("old") or "").strip(), (body.get("new") or "").strip()
    if not field or not old or not new:
        return {"ok": False, "msg": "参数不完整"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(f"UPDATE `{TABLE_NAME}` SET `{field}`=%s WHERE `{field}`=%s", (new, old))
    conn.commit()
    n = cur.rowcount
    cur.close()
    conn.close()
    reload_data()
    return {"ok": True, "msg": f"已将「{old}」重命名为「{new}」（{n} 条）"}


def api_category_delete(q, body, user):
    field = FIELD_MAP.get(body.get("field"))
    name = (body.get("name") or "").strip()
    if not field or not name:
        return {"ok": False, "msg": "参数不完整"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(f"DELETE FROM `{TABLE_NAME}` WHERE `{field}`=%s", (name,))
    conn.commit()
    n = cur.rowcount
    cur.close()
    conn.close()
    reload_data()
    return {"ok": True, "msg": f"已删除分类「{name}」及其 {n} 条记录"}


def api_quality(q, body, user):
    """贸易信息维护：数据质量检查"""
    if DF.empty:
        return {"total": 0, "null_rows": 0, "dup_rows": 0, "bad_date": 0}
    null_rows = int(DF[["统计日期", "贸易类型", "国家类型", "进出口总额(万元)"]].isna().any(axis=1).sum())
    dup_rows = int(DF.duplicated(subset=["统计日期", "贸易类型", "国家类型", "进出口总额(万元)"]).sum())
    bad_date = int(DF["统计日期"].isna().sum())
    return {"total": int(len(DF)), "null_rows": null_rows,
            "dup_rows": dup_rows, "bad_date": bad_date}


def api_quality_clean(q, body, user):
    """清理关键字段为空的脏数据"""
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(
        f"DELETE FROM `{TABLE_NAME}` WHERE 统计日期 IS NULL OR 贸易类型 IS NULL "
        "OR 国家类型 IS NULL OR 进出口总额 IS NULL")
    conn.commit()
    n = cur.rowcount
    cur.close()
    conn.close()
    reload_data()
    return {"ok": True, "msg": f"已清理无效数据 {n} 条"}


# ================================================================
# 模块三：贸易查询（搜索 / 地区筛选 / 类型筛选 / 占比筛选 / 详情查看）
# —— 复用 api_records 的筛选参数；详情用 api_record_detail
# ================================================================

# ================================================================
# 模块四：数据分析与可视化（占比 / 增长 / 金额 / 热度 / 地区分布 / 类型分析）
# ================================================================
def api_summary(q=None, body=None, user=None):
    if DF.empty:
        return {"total": 0, "amount": 0, "avg_pct": 0, "avg_growth": 0,
                "types": 0, "countries": 0, "max_amount": 0, "max_date": ""}
    max_row = DF.loc[DF["进出口总额(万元)"].idxmax()]
    return {
        "total": int(len(DF)),
        "amount": round(float(DF["进出口总额(万元)"].sum()), 2),
        "avg_pct": round(float(DF["占比(%)"].mean()), 2),
        "avg_growth": round(float(DF["同比增长(%)"].mean()), 2),
        "types": int(DF["贸易类型"].nunique()),
        "countries": int(DF["国家类型"].nunique()),
        "max_amount": round(float(max_row["进出口总额(万元)"]), 2),
        "max_date": max_row["统计日期"].strftime("%Y-%m-%d"),
    }


def api_trade_type(q=None, body=None, user=None):
    if DF.empty:
        return {"types": [], "amounts": [], "pcts": []}
    g = DF.groupby("贸易类型").agg(
        amount=("进出口总额(万元)", "sum"), pct=("占比(%)", "mean"),
    ).round(2).sort_values("amount", ascending=False)
    return {"types": g.index.tolist(), "amounts": g["amount"].tolist(),
            "pcts": g["pct"].tolist()}


def api_country_type(q=None, body=None, user=None):
    if DF.empty:
        return {"countries": [], "amounts": [], "pcts": []}
    g = DF.groupby("国家类型").agg(
        amount=("进出口总额(万元)", "sum"), pct=("占比(%)", "mean"),
    ).round(2).sort_values("amount", ascending=False)
    return {"countries": g.index.tolist(), "amounts": g["amount"].tolist(),
            "pcts": g["pct"].tolist()}


def api_monthly(q=None, body=None, user=None):
    if DF.empty:
        return {"months": [], "pcts": [], "amounts": []}
    g = DF.groupby("月份").agg(
        pct=("占比(%)", "mean"), amount=("进出口总额(万元)", "sum"),
    ).round(2).sort_index()
    return {"months": [f"{int(m)}月" for m in g.index],
            "pcts": g["pct"].tolist(), "amounts": g["amount"].tolist()}


def api_growth(q=None, body=None, user=None):
    if DF.empty:
        return {"bins": [], "counts": []}
    bins = [-10, 0, 5, 10, 15, 20, 25]
    labels = ["-10~0%", "0~5%", "5~10%", "10~15%", "15~20%", "20~25%"]
    s = pd.cut(DF["同比增长(%)"], bins=bins, labels=labels)
    counts = s.value_counts().sort_index().tolist()
    return {"bins": labels, "counts": counts}


def api_cross(q=None, body=None, user=None):
    if DF.empty:
        return {"types": [], "countries": [], "series": []}
    pivot = DF.pivot_table(values="进出口总额(万元)", index="贸易类型",
                           columns="国家类型", aggfunc="sum", fill_value=0).round(2)
    return {"types": pivot.index.tolist(), "countries": pivot.columns.tolist(),
            "series": [{"name": c, "data": pivot[c].tolist()} for c in pivot.columns]}


def api_hot(q=None, body=None, user=None):
    """热度分析：各类型/地区出现频次与平均增长"""
    if DF.empty:
        return {"type_names": [], "type_counts": [], "type_growth": [],
                "country_names": [], "country_counts": [], "country_growth": []}
    t = DF.groupby("贸易类型").agg(n=("record_id", "count"),
                                   g=("同比增长(%)", "mean")).round(2).sort_values("n", ascending=False)
    c = DF.groupby("国家类型").agg(n=("record_id", "count"),
                                   g=("同比增长(%)", "mean")).round(2).sort_values("n", ascending=False)
    return {
        "type_names": t.index.tolist(), "type_counts": t["n"].tolist(),
        "type_growth": t["g"].tolist(),
        "country_names": c.index.tolist(), "country_counts": c["n"].tolist(),
        "country_growth": c["g"].tolist(),
    }


# ================================================================
# 模块五：系统管理（公告 / 数据统计 / 数据维护 / 系统信息）
# ================================================================
def api_announcements(q, body, user):
    """公告列表（分页）"""
    page = max(1, int(q.get("page", ["1"])[0]))
    size = max(1, min(int(q.get("size", ["10"])[0]), 100))
    conn = db_connect()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT COUNT(*) FROM announcements")
    total = cur.fetchone()["COUNT(*)"]
    cur.execute(
        "SELECT id,title,category,image,summary,content,clicks,publisher,created_at "
        "FROM announcements ORDER BY id DESC LIMIT %s OFFSET %s",
        (size, (page - 1) * size))
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return {"total": int(total), "page": page, "size": size, "announcements": rows}


def api_announcement_detail(q, body, user):
    """查看公告详情，点击率 +1"""
    rid = q.get("id", [None])[0]
    conn = db_connect()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("UPDATE announcements SET clicks=clicks+1 WHERE id=%s", (rid,))
    conn.commit()
    cur.execute("SELECT * FROM announcements WHERE id=%s", (rid,))
    row = cur.fetchone()
    cur.close()
    conn.close()
    if not row:
        return {"error": "公告不存在"}, 404
    return {"announcement": row}


def api_announcements_add(q, body, user):
    title = (body.get("title") or "").strip()
    if not title:
        return {"ok": False, "msg": "标题不能为空"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO announcements (title,category,image,summary,content,publisher) "
        "VALUES (%s,%s,%s,%s,%s,%s)",
        (title, body.get("category") or "通知", body.get("image") or "",
         body.get("summary") or "", body.get("content") or "",
         user.get("nickname") or user["username"]))
    conn.commit()
    cur.close()
    conn.close()
    return {"ok": True, "msg": "公告已发布"}


def api_announcements_update(q, body, user):
    rid = body.get("id")
    if not rid:
        return {"ok": False, "msg": "缺少 id"}, 400
    conn = db_connect()
    cur = conn.cursor()
    cur.execute(
        "UPDATE announcements SET title=%s, category=%s, image=%s, summary=%s, content=%s "
        "WHERE id=%s",
        (body.get("title") or "", body.get("category") or "通知",
         body.get("image") or "", body.get("summary") or "",
         body.get("content") or "", rid))
    conn.commit()
    cur.close()
    conn.close()
    return {"ok": True, "msg": "公告已更新"}


def api_announcements_import(q, body, user):
    """批量导入：body.items 为公告数组 [{标题,类别,摘要,内容}]"""
    items = body.get("items") or []
    if not items:
        return {"ok": False, "msg": "没有可导入的数据"}, 400
    conn = db_connect()
    cur = conn.cursor()
    n = 0
    for it in items:
        title = str(it.get("标题") or it.get("title") or "").strip()
        if not title:
            continue
        cur.execute(
            "INSERT INTO announcements (title,category,summary,content,publisher) "
            "VALUES (%s,%s,%s,%s,%s)",
            (title, it.get("类别") or "通知", it.get("摘要") or "",
             it.get("内容") or "", user.get("nickname") or user["username"]))
        n += 1
    conn.commit()
    cur.close()
    conn.close()
    return {"ok": True, "msg": f"成功导入 {n} 条公告"}


def api_announcements_delete(q, body, user):
    rid = body.get("id") or (q.get("id", [None])[0])
    conn = db_connect()
    cur = conn.cursor()
    cur.execute("DELETE FROM announcements WHERE id=%s", (rid,))
    conn.commit()
    cur.close()
    conn.close()
    return {"ok": True, "msg": "公告已删除"}


def api_stats(q=None, body=None, user=None):
    csv_rows = 0
    if os.path.exists(CSV_PATH):
        csv_rows = len(pd.read_csv(CSV_PATH))
    users = anns = 0
    mysql_ok = True
    try:
        conn = db_connect()
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM users")
        users = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM announcements")
        anns = cur.fetchone()[0]
        cur.close()
        conn.close()
    except Exception:
        mysql_ok = False
    s = api_summary()
    return {
        "records": s["total"], "users": int(users), "announcements": int(anns),
        "types": s["types"], "countries": s["countries"],
        "csv_rows": int(csv_rows), "mysql_ok": mysql_ok,
    }


def api_system_info(q=None, body=None, user=None):
    import platform as pf
    import sys
    mysql_ver = ""
    try:
        conn = db_connect()
        cur = conn.cursor()
        cur.execute("SELECT VERSION()")
        mysql_ver = cur.fetchone()[0]
        cur.close()
        conn.close()
    except Exception:
        mysql_ver = "不可用"
    uptime = int(time.time() - START_TIME)
    h, m = uptime // 3600, (uptime % 3600) // 60
    return {
        "app": f"大连市海外贸易数据采集与可视化系统 {APP_VERSION}",
        "python": sys.version.split()[0],
        "platform": f"{pf.system} {pf.release}",
        "mysql": mysql_ver,
        "datasource": "MySQL" if not DF.empty else "无",
        "records": int(len(DF)),
        "uptime": f"{h}小时{m}分钟",
        "port": PORT,
    }


def api_maintenance_import(q, body, user):
    try:
        n = import_csv_to_mysql()
        return {"ok": True, "msg": f"CSV 已重新导入 MySQL（{n} 条），数据已刷新"}
    except Exception as e:
        return {"ok": False, "msg": f"导入失败：{e}"}, 500


def api_maintenance_reload(q, body, user):
    reload_data()
    return {"ok": True, "msg": f"数据已重新加载（{len(DF)} 条）"}


# ===================== 路由表 =====================
# (method, path): (handler, admin_only)
ROUTES = {
    # 模块一：用户管理
    ("POST", "/api/register"): (api_register, False),
    ("POST", "/api/login"): (api_login, False),
    ("POST", "/api/logout"): (api_logout, False),
    ("GET", "/api/profile"): (api_profile, False),
    ("POST", "/api/profile"): (api_profile_update, False),
    ("GET", "/api/users"): (api_users, True),
    ("POST", "/api/users"): (api_users_add, True),
    ("DELETE", "/api/users"): (api_users_delete, True),
    # 模块二：贸易信息管理
    ("GET", "/api/records"): (api_records, False),
    ("GET", "/api/records/detail"): (api_record_detail, False),
    ("POST", "/api/records"): (api_records_add, False),
    ("PUT", "/api/records"): (api_records_update, False),
    ("DELETE", "/api/records"): (api_records_delete, False),
    ("GET", "/api/categories"): (api_categories, False),
    ("POST", "/api/categories/rename"): (api_category_rename, False),
    ("POST", "/api/categories/delete"): (api_category_delete, False),
    ("GET", "/api/quality"): (api_quality, False),
    ("POST", "/api/quality/clean"): (api_quality_clean, False),
    # 模块四：数据分析与可视化
    ("GET", "/api/summary"): (api_summary, False),
    ("GET", "/api/trade_type"): (api_trade_type, False),
    ("GET", "/api/country_type"): (api_country_type, False),
    ("GET", "/api/monthly"): (api_monthly, False),
    ("GET", "/api/growth"): (api_growth, False),
    ("GET", "/api/cross"): (api_cross, False),
    ("GET", "/api/hot"): (api_hot, False),
    # 模块五：系统管理
    ("GET", "/api/announcements"): (api_announcements, False),
    ("GET", "/api/announcements/detail"): (api_announcement_detail, False),
    ("POST", "/api/announcements"): (api_announcements_add, True),
    ("PUT", "/api/announcements"): (api_announcements_update, True),
    ("POST", "/api/announcements/import"): (api_announcements_import, True),
    ("DELETE", "/api/announcements"): (api_announcements_delete, True),
    ("GET", "/api/stats"): (api_stats, False),
    ("GET", "/api/system_info"): (api_system_info, False),
    ("POST", "/api/maintenance/import_csv"): (api_maintenance_import, True),
    ("POST", "/api/maintenance/reload"): (api_maintenance_reload, False),
}

PUBLIC_PATHS = {("POST", "/api/login"), ("POST", "/api/register")}


# ===================== HTTP 请求处理 =====================
class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def _send_json(self, data, status=200):
        body = json.dumps(data, ensure_ascii=False, default=str).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_file(self, filepath, content_type):
        if not os.path.exists(filepath):
            self._send_json({"error": "file not found"}, status=404)
            return
        with open(filepath, "rb") as f:
            body = f.read()
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0:
            return {}
        try:
            return json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception:
            return {}

    def _get_user(self):
        token = self.headers.get("X-Token") or ""
        return token, SESSIONS.get(token)

    def _handle(self, method):
        parsed = urlparse(self.path)
        path = parsed.path

        # 静态页面
        if method == "GET":
            if path in ("/", "/index.html"):
                self._send_file(os.path.join(BASE_DIR, "templates", "index.html"),
                                "text/html; charset=utf-8")
                return
            if path == "/login.html":
                self._send_file(os.path.join(BASE_DIR, "templates", "login.html"),
                                "text/html; charset=utf-8")
                return

        # 公告导入模板下载（CSV 文件，带 BOM 兼容 Excel 中文）
        if method == "GET" and path == "/api/announcements/template":
            token, user = self._get_user()
            if not user:
                self._send_json({"ok": False, "msg": "未登录"}, status=401)
                return
            body = ("﻿标题,类别,摘要,内容\r\n"
                    "示例公告标题,通知,这是摘要内容,这是公告正文\r\n").encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/csv; charset=utf-8")
            self.send_header("Content-Disposition",
                             "attachment; filename=announcement_template.csv")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        # API 路由
        key = (method, path)
        if key in ROUTES:
            fn, admin_only = ROUTES[key]
            token, user = self._get_user()
            if key not in PUBLIC_PATHS and not user:
                self._send_json({"ok": False, "msg": "未登录或登录已过期"}, status=401)
                return
            if admin_only and user.get("role") != "admin":
                self._send_json({"ok": False, "msg": "需要管理员权限"}, status=403)
                return
            q = parse_qs(parsed.query) if parsed.query else {}
            body = self._read_body() if method in ("POST", "PUT", "DELETE") else {}
            if user:
                body["_token"] = token
            try:
                result = fn(q, body, user)
                if isinstance(result, tuple):
                    self._send_json(result[0], status=result[1])
                else:
                    self._send_json(result)
            except Exception as e:
                self._send_json({"ok": False, "msg": f"服务器错误：{e}"}, status=500)
            return

        self._send_json({"error": "not found"}, status=404)

    def do_GET(self):
        self._handle("GET")

    def do_POST(self):
        self._handle("POST")

    def do_PUT(self):
        self._handle("PUT")

    def do_DELETE(self):
        self._handle("DELETE")


# ===================== 启动 =====================
if __name__ == "__main__":
    print("=" * 60)
    print("  大连市海外贸易数据采集与可视化系统（五大模块）")
    print("=" * 60)
    print(f"  数据记录数：{len(DF)}")
    print(f"  服务地址：http://localhost:{PORT}")
    print("  登录页：http://localhost:5000/login.html")
    print("  默认管理员：admin / admin123")
    print("  按 Ctrl+C 停止服务")
    print("=" * 60)
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n服务已停止")
        server.server_close()
