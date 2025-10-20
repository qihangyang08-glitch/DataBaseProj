import React, {
  useState,
  useEffect,
  createContext,
  useContext,
  useCallback,
  useMemo,
  useRef,
} from "react";
import { createPortal } from "react-dom";
import axios from "axios";
import {
  ChevronLeft,
  ChevronRight,
  Menu,
  Settings,
  User,
  Plus,
  Search,
  Calendar as CalendarIcon,
  Edit3,
  MoreVertical,
  X,
  Check,
  Clock,
  BookOpen,
  ArrowLeft,
  Clipboard,
  CheckCircle,
  Bell,
  XCircle,
} from "lucide-react";

// Debug: module loaded
console.log("[frontend] src/App.jsx module loaded");

// Mock API service with enhanced functionality
//const API_BASE = 'http://localhost:8080/api';
const API_BASE = "/api";

// Safe response parser: returns parsed JSON or { raw: text } on parse failure, or {} when empty.
async function safeParseResponse(res) {
  try {
    const text = await res.text();
    if (!text) return {};
    try {
      return JSON.parse(text);
    } catch {
      return { raw: text };
    }
  } catch {
    return { rawError: 'unknown' };
  }
}

const api = {
  getAuthHeaders: () => {
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }
    const headers = {};
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;
    return headers;
  },
  // Auth calls use real backend endpoints. Normalize token field names (accessToken / token / access_token).
  login: async (credentials) => {
    if (!credentials || !credentials.username || !credentials.password) {
      const e = new Error("用户名或密码不能为空");
      e.status = 400;
      throw e;
    }
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
    });
    const data = await safeParseResponse(res);
    if (!res.ok) {
      const err = new Error(
        data?.message || data?.error || res.statusText || "登录失败"
      );
      err.status = res.status;
      err.data = data;
      throw err;
    }
    const token =
      data.accessToken ||
      data.token ||
      data.access_token ||
      data.data?.accessToken ||
      null;
    return { user: data.user || data.data || null, token };
  },
  register: async (userData) => {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userData),
    });
    const data = await safeParseResponse(res);
    if (!res.ok) {
      const err = new Error(
        data?.message || data?.error || res.statusText || "注册失败"
      );
      err.status = res.status;
      err.data = data;
      throw err;
    }
    const token =
      data.accessToken ||
      data.token ||
      data.access_token ||
      data.data?.accessToken ||
      null;
    return { user: data.user || data.data || null, token };
  },
  getCalendarData: async (year, month) => {
    // year: number, month: 1-12
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }
    const headers = { "Content-Type": "application/json" };
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;

    try {
      // Build relative URL (API_BASE may be '/api')
      const params = new URLSearchParams();
      if (year) params.set("year", String(year));
      if (month) params.set("month", String(month));
      const url = `${API_BASE}/calendar${
        params.toString() ? "?" + params.toString() : ""
      }`;
      const res = await fetch(url, { headers });
      const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to fetch calendar"
        );
      // backend likely returns { tasks: [...] } or { data: { tasks: [...] } } or array
      let rawTasks = [];
      if (Array.isArray(data)) rawTasks = data;
      else rawTasks = data.tasks || data.data?.tasks || data.data || [];

      // normalize fields for createdDate and dueDate
      const tasks = (rawTasks || []).map((t) => {
        const task = { ...t };
        // createdDate may be createdAt / created_date / created
        if (!task.createdDate) {
          if (task.createdAt)
            task.createdDate = dateUtils.formatDateString(task.createdAt);
          else if (task.created)
            task.createdDate = dateUtils.formatDateString(task.created);
          else if (task.createDate)
            task.createdDate = dateUtils.formatDateString(task.createDate);
        } else {
          // ensure YYYY-MM-DD
          try {
            task.createdDate = dateUtils.formatDateString(task.createdDate);
          } catch {
            /* ignore */
          }
        }

        // dueDate selection logic:
        // - CLASS tasks: calendar should display personalDeadline if present, otherwise official deadline
        // - PERSONAL tasks: use official deadline
        const type = (task.taskType || task.type || "")
          .toString()
          .toUpperCase();
        if (type === "CLASS") {
          task.dueDate =
            task.personalDeadline || task.deadline || task.due || null;
        } else {
          task.dueDate = task.deadline || task.due || null;
        }

        return task;
      });

      return { tasks };
    } catch (err) {
      console.error("api.getCalendarData error", err);
      return { tasks: [] };
    }
  },
  updateTask: async (taskId, updates) => {
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }
    const headers = { "Content-Type": "application/json" };
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;

    try {
      console.debug(
        "[api.updateTask] PUT",
        `${API_BASE}/tasks/${taskId}`,
        "payload:",
        updates
      );
      const res = await fetch(`${API_BASE}/tasks/${taskId}`, {
        method: "PUT",
        headers,
        body: JSON.stringify(updates),
      });
      const data = await safeParseResponse(res);
      if (!res.ok) {
        const message =
          data?.message ||
          data?.error ||
          res.statusText ||
          "Failed to update task";
        const err = new Error(message);
        err.status = res.status;
        err.raw = data;
        console.error(
          "api.updateTask server error",
          err,
          "response data:",
          data
        );
        throw err;
      }
      console.debug("[api.updateTask] response", res.status, data);
      return { success: true, task: data.task || data };
    } catch (err) {
      // network or parsing error
      console.error("api.updateTask error", err);
      throw err;
    }
  },
  deleteTask: async (taskId) => {
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }
    const headers = {};
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;

    try {
      const res = await fetch(`${API_BASE}/tasks/${taskId}`, {
        method: "DELETE",
        headers,
      });
      if (!res.ok) {
      const data = await safeParseResponse(res);
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to delete task"
        );
      }
      return { success: true };
    } catch (err) {
      console.error("api.deleteTask error", err);
      throw err;
    }
  },
  createTask: async (task) => {
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }
    const headers = { "Content-Type": "application/json" };
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;

    try {
      // Determine endpoint: if task.classId present then POST /api/classes/{id}/tasks else POST /api/tasks/personal
      let url = `${API_BASE}/tasks/personal`;
      if (task.classId) url = `${API_BASE}/classes/${task.classId}/tasks`;
      const res = await fetch(url, {
        method: "POST",
        headers,
        body: JSON.stringify(task),
      });
      const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to create task"
        );
      return { success: true, task: data.task || data };
    } catch (err) {
      console.error("api.createTask error", err);
      throw err;
    }
  },
  updateTaskStatus: async (taskId, payload, asUserRelation = false) => {
    // asUserRelation: when true, update personal relation endpoint
    const headers = {
      "Content-Type": "application/json",
      ...api.getAuthHeaders(),
    };
    try {
      let url = `${API_BASE}/tasks/${taskId}`;
      if (asUserRelation) url = `${API_BASE}/tasks/${taskId}/status`;
      console.debug("[api.updateTaskStatus] PUT", url, "payload:", payload);

      const res = await axios.put(url, payload, { headers });
      console.debug("[api.updateTaskStatus] response", res.status, res.data);
      return { success: true, data: res.data };
    } catch (err) {
      console.error("api.updateTaskStatus error", err);
      const respData = err?.response?.data;
      return {
        success: false,
        message: respData?.message || respData || err.message,
        raw: respData,
      };
    }
  },
  joinClass: async (inviteCode) => {
    // First, find class by invite code
    try {
      const findHeaders = api.getAuthHeaders();
      const findRes = await fetch(
        `${API_BASE}/classes/search?inviteCode=${encodeURIComponent(
          inviteCode
        )}`,
        { headers: findHeaders }
      );
  const findData = await safeParseResponse(findRes);
      if (!findRes.ok)
        throw new Error(findData?.message || findData?.error || "未找到班级");
      const cls = findData?.data || findData;
      const classId = cls?.id || cls?.data?.id;
      if (!classId) throw new Error("未能解析到班级ID");

      const headers = {
        "Content-Type": "application/json",
        ...api.getAuthHeaders(),
      };

      // POST /api/classes/{classId}/join - backend expects ClassJoinRequestDto
      const res = await fetch(`${API_BASE}/classes/${classId}/join`, {
        method: "POST",
        headers,
        body: JSON.stringify({ joinReason: "通过前端加入" }),
      });
      const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(data?.message || data?.error || "服务器内部错误");
      return { success: true, message: data?.message || "已申请加入" };
    } catch (err) {
      console.error("api.joinClass error", err);
      return { success: false, message: err.message || "加入班级失败" };
    }
  },
  getClassTasks: async (classId, page = 0, size = 20) => {
    const headers = api.getAuthHeaders();

    try {
      const res = await fetch(
        `${API_BASE}/classes/${classId}/tasks?page=${page}&size=${size}`,
        { headers }
      );
      const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to get class tasks"
        );
      // backend returns ApiResponse with data being a Page
      const payload = data?.data ?? data;
      if (payload && Array.isArray(payload?.content))
        return { tasks: payload.content };
      if (Array.isArray(payload)) return { tasks: payload };
      return { tasks: [] };
    } catch (err) {
      console.error("api.getClassTasks error", err);
      return { tasks: [] };
    }
  },
  checkClassPermission: async (classId) => {
    try {
      const headers = {
        "Content-Type": "application/json",
        ...api.getAuthHeaders(),
      };
      const res = await fetch(`${API_BASE}/classes/${classId}/my-role`, {
        headers,
      });
  const data = await safeParseResponse(res);
      if (!res.ok) {
        throw new Error(data?.message || "Failed to fetch role info");
      }
      const payload = data?.data ?? data;
      // we expect payload.canPublishTasks boolean per your spec
      return { ok: true, canCreateTask: !!payload?.canPublishTasks, payload };
    } catch (err) {
      console.error("checkClassPermission error", err);
      return { ok: false, error: err.message || String(err) };
    }
  },
  syncClass: async (classId, range = "month") => {
    const headers = {
      "Content-Type": "application/json",
      ...api.getAuthHeaders(),
    };

    try {
      const res = await fetch(
        `${API_BASE}/sync/class/${classId}?range=${encodeURIComponent(range)}`,
        {
          method: "POST",
          headers,
        }
      );
  const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to sync class"
        );
      return { success: true, result: data.data || data };
    } catch (err) {
      console.error("api.syncClass error", err);
      return { success: false, message: err.message || "同步失败" };
    }
  },
  createClass: async (classData) => {
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }
    const headers = { "Content-Type": "application/json" };
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;

    try {
      const res = await fetch(`${API_BASE}/classes`, {
        method: "POST",
        headers,
        body: JSON.stringify(classData),
      });
  const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to create class"
        );
      return { success: true, class: data.class || data };
    } catch (err) {
      console.error("api.createClass error", err);
      return { success: false, message: err.message || "创建班级失败" };
    }
  },
  getClasses: async () => {
    // Try to read token (may be JSON-stringified)
    let raw = localStorage.getItem("token");
    let token = null;
    try {
      token = raw ? JSON.parse(raw) : null;
    } catch {
      token = raw;
    }

    const headers = {};
    if (token)
      headers["Authorization"] =
        typeof token === "string" && token.startsWith("Bearer")
          ? token
          : `Bearer ${token}`;

    try {
      const res = await fetch(`${API_BASE}/classes/my`, { headers });
  const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to fetch classes"
        );
      // backend returns ApiResponse with data possibly being a Page (contains content array)
      // Normalize to an array of class DTOs
      const payload = data?.data ?? data;
      if (Array.isArray(payload)) return { classes: payload };
      if (payload && Array.isArray(payload.content))
        return { classes: payload.content };
      return { classes: [] };
    } catch (err) {
      console.error("api.getClasses error", err);
      return { classes: [] };
    }
  },
  // Search public classes by name with pagination
  searchClasses: async (name, page = 0, size = 20, sort) => {
    if (!name || !name.toString().trim()) return { classes: [], page: { number: 0, totalPages: 0, totalElements: 0 } };
    const headers = api.getAuthHeaders();
    try {
      const params = new URLSearchParams();
      params.set('name', String(name));
      params.set('page', String(page || 0));
      params.set('size', String(size || 20));
      if (sort) params.set('sort', sort);
      const res = await fetch(`${API_BASE}/classes/search-by-name?${params.toString()}`, { headers });
      const data = await safeParseResponse(res);
      if (!res.ok) throw new Error(data?.message || '搜索失败');
      const payload = data?.data ?? data;
      return { classes: payload?.content || [], page: payload };
    } catch (err) {
      console.error('api.searchClasses error', err);
      return { classes: [], page: { number: 0, totalPages: 0, totalElements: 0 }, error: err.message || String(err) };
    }
  },
  joinClassById: async (classId, reason = '通过搜索申请加入') => {
    try {
      const headers = { 'Content-Type': 'application/json', ...api.getAuthHeaders() };
      const res = await fetch(`${API_BASE}/classes/${classId}/join`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ joinReason: reason }),
      });
      const data = await safeParseResponse(res);
      if (!res.ok) throw new Error(data?.message || '申请加入失败');
      return { success: true, message: data?.message || '已提交申请' };
    } catch (err) {
      console.error('api.joinClassById error', err);
      return { success: false, message: err.message || String(err) };
    }
  },
  getClassMembers: async (classId, page = 0, size = 20) => {
    const headers = api.getAuthHeaders();
    try {
      const res = await fetch(
        `${API_BASE}/classes/${classId}/members?page=${page}&size=${size}`,
        { headers }
      );
  const data = await safeParseResponse(res);
      if (!res.ok) throw new Error(data?.message || "Failed to fetch members");
      const payload = data?.data ?? data;
      if (payload && Array.isArray(payload.content))
        return { members: payload.content };
      if (Array.isArray(payload)) return { members: payload };
      return { members: [] };
    } catch (err) {
      console.error("api.getClassMembers error", err);
      return { members: [] };
    }
  },

  getCurrentUserPermissions: async (classId) => {
    const headers = api.getAuthHeaders();
    try {
      // Corrected the endpoint from /permissions/me to /my-role as per your instruction
      const res = await fetch(`${API_BASE}/classes/${classId}/my-role`, {
        headers,
      });
  const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(data?.message || "Failed to get permissions");
      // The logic here correctly extracts the nested "data" object which contains the "role" field
      return { permissions: data.data || data };
    } catch (err) {
      console.error("api.getCurrentUserPermissions error", err);
      return { permissions: null };
    }
  },

  promoteMember: async (classId, userId) => {
    const headers = {
      "Content-Type": "application/json",
      ...api.getAuthHeaders(),
    };
    try {
      const res = await fetch(
        `${API_BASE}/classes/${classId}/members/${userId}/promote`,
        {
          method: "PUT",
          headers,
        }
      );
      if (!res.ok) {
        const data = await safeParseResponse(res);
        throw new Error(data?.message || "Failed to promote member");
      }
      return { success: true };
    } catch (err) {
      console.error("api.promoteMember error", err);
      throw err;
    }
  },

  demoteMember: async (classId, userId) => {
    const headers = {
      "Content-Type": "application/json",
      ...api.getAuthHeaders(),
    };
    try {
      const res = await fetch(
        `${API_BASE}/classes/${classId}/members/${userId}/demote`,
        {
          method: "PUT",
          headers,
        }
      );
      if (!res.ok) {
  const data = await safeParseResponse(res);
        throw new Error(data?.message || "Failed to demote member");
      }
      return { success: true };
    } catch (err) {
      console.error("api.demoteMember error", err);
      throw err;
    }
  },
  getPendingApprovals: async (page = 0, size = 20) => {
    const headers = api.getAuthHeaders();
    try {
      const res = await fetch(`${API_BASE}/approvals/pending?page=${page}&size=${size}`, { headers });
  const data = await safeParseResponse(res);
      if (!res.ok) throw new Error(data?.message || 'Failed to fetch pending approvals');
      
      // API returns a Page object within the "data" field
      const pageData = data.data || data;
      return {
        approvals: pageData.content || [],
        total: pageData.totalElements || 0,
      };
    } catch (err) {
      console.error('api.getPendingApprovals error', err);
      return { approvals: [], total: 0 };
    }
  },

  processApproval: async (classId, userId, action) => {
    // action should be 'APPROVE' or 'REJECT'
    const headers = { 'Content-Type': 'application/json', ...api.getAuthHeaders() };
    try {
      const res = await fetch(`${API_BASE}/classes/${classId}/approvals/${userId}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({ action }),
      });
      if (!res.ok) {
  const data = await safeParseResponse(res);
        throw new Error(data?.message || `Failed to ${action.toLowerCase()} approval`);
      }
      return { success: true };
    } catch (err) {
      console.error('api.processApproval error', err);
      throw err;
    }
  },
  getCurrentUser: async () => {
    const headers = {
      "Content-Type": "application/json",
      ...api.getAuthHeaders(),
    };
    try {
      const res = await fetch(`${API_BASE}/users/me`, { headers });
  const data = await safeParseResponse(res);
      if (!res.ok)
        throw new Error(
          data?.message ||
            data?.error ||
            res.statusText ||
            "Failed to fetch current user"
        );
      // ApiResponse { data: { ... } } or direct user object
      const payload = data?.data ?? data;
      return { user: payload };
    } catch (err) {
      console.error("api.getCurrentUser error", err);
      throw err;
    }
  },
};

const JoinClassForm = ({ onClose, onJoined }) => {
  const [inviteCode, setInviteCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await api.joinClass(inviteCode);
      if (res && res.success) {
        // some backends return class object when immediate join, others return pending status
        if (res.class) {
          if (onJoined) onJoined(res.class);
        } else {
          // inform user that join request is pending
          setError(
            "已提交申请，等待管理员审核（若后台已自动申请则刷新侧栏查看）"
          );
        }
        onClose();
      } else {
        setError(res?.message || "加入班级失败");
      }
    } catch (err) {
      console.error(err);
      setError(err.message || "加入班级时发生错误");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          邀请码
        </label>
        <input
          type="text"
          value={inviteCode}
          onChange={(e) => setInviteCode(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
          placeholder="请输入邀请码"
          required
        />
      </div>

      {error && (
        <div className="text-sm text-red-600 bg-red-50 border border-red-100 p-2 rounded">
          {error}
        </div>
      )}

      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 bg-gray-100 rounded text-gray-700"
        >
          取消
        </button>
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
          disabled={loading}
        >
          {loading ? "加入中..." : "加入"}
        </button>
      </div>
    </form>
  );
};

// Create class form (was missing) - posts to backend and calls onCreated with created class
const CreateClassForm = ({ onClose, onCreated }) => {
  const [form, setForm] = useState({ name: "", description: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.name || form.name.trim().length === 0) {
      setError("班级名称不能为空");
      return;
    }
    setLoading(true);
    try {
      const res = await api.createClass(form);
      if (res && (res.success || res.class)) {
        const cls = res.class || res;
        if (onCreated) onCreated(cls);
        onClose();
      } else {
        setError(res?.message || "创建失败");
      }
    } catch (err) {
      console.error(err);
      setError(err.message || "创建班级出错");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          班级名称
        </label>
        <input
          type="text"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg"
          placeholder="输入班级名称"
          required
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          描述（可选）
        </label>
        <textarea
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg"
          placeholder="班级描述"
        />
      </div>

      {error && <div className="text-sm text-red-600">{error}</div>}

      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 bg-gray-100 rounded"
        >
          取消
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2 bg-blue-600 text-white rounded"
        >
          {loading ? "创建中..." : "创建"}
        </button>
      </div>
    </form>
  );
};
const ClassMembersModal = ({
  isOpen,
  onClose,
  classItem,
  currentUserRole,
  currentUser,
}) => {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);

  // 1. 数据获取逻辑保持不变
  const fetchMembers = useCallback(async () => {
    if (!classItem) return;
    setLoading(true);
    try {
      const res = await api.getClassMembers(classItem.id);
      setMembers(res.members || []);
    } catch (err) {
      console.error("Failed to load members", err);
    } finally {
      setLoading(false);
    }
  }, [classItem]);

  useEffect(() => {
    if (isOpen) {
      fetchMembers();
    }
  }, [isOpen, fetchMembers]);

  // 2. 新增：处理角色标签点击的核心函数
  const handleRoleClick = async (member) => {
    // 权限判断已在 getRoleBadge 中完成，这里直接执行操作
    if (member.role === "MEMBER") {
      if (
        window.confirm(
          `确定要任命 "${member.displayName || member.username}" 为管理员吗？`
        )
      ) {
        try {
          await api.promoteMember(classItem.id, member.userId);
          fetchMembers(); // 成功后刷新列表
        } catch (err) {
          alert("任命失败: " + err.message);
        }
      }
    } else if (member.role === "ADMIN") {
      if (
        window.confirm(
          `确定要将 "${member.displayName || member.username}" 降级为成员吗？`
        )
      ) {
        try {
          await api.demoteMember(classItem.id, member.userId);
          fetchMembers(); // 成功后刷新列表
        } catch (err) {
          alert("降级失败: " + err.message);
        }
      }
    }
  };

  // 3. 修改：让 getRoleBadge 函数变得可交互
  const getRoleBadge = (member) => {
    const roles = {
      OWNER: { text: "创建者", color: "bg-red-100 text-red-800" },
      ADMIN: { text: "管理员", color: "bg-purple-100 text-purple-800" },
      MEMBER: { text: "成员", color: "bg-gray-100 text-gray-800" },
    };
    const config = roles[member.role] || roles.MEMBER;

    // 权限判断：当前用户是OWNER，且操作的不是自己，也不是其他OWNER
    const isOwner = currentUserRole === "OWNER";
    const isSelf = currentUser?.userId === member.userId;
    const isClickable = isOwner && !isSelf && member.role !== "OWNER";

    if (isClickable) {
      // 如果可点击，渲染成一个按钮
      return (
        <button
          onClick={() => handleRoleClick(member)}
          className={`px-2 py-1 text-xs font-medium rounded-full ${config.color} cursor-pointer hover:opacity-80 transition-opacity`}
          title={
            member.role === "MEMBER" ? "点击任命为管理员" : "点击降级为成员"
          }
        >
          {config.text}
        </button>
      );
    } else {
      // 否则，渲染成普通的静态文本
      return (
        <span
          className={`px-2 py-1 text-xs font-medium rounded-full ${config.color}`}
        >
          {config.text}
        </span>
      );
    }
  };

  return (
    // 4. 移除所有旧的右键菜单(ContextMenu)相关代码，简化Modal
    <Modal isOpen={isOpen} onClose={onClose} title="班级成员">
      <div className="min-h-[300px]">
        {loading ? (
          <div className="flex justify-center items-center h-full">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
          </div>
        ) : members.length === 0 ? (
          <div className="text-center text-gray-500 py-10">暂无成员</div>
        ) : (
          <ul className="space-y-2">
            {members.map((member) => (
              <li
                key={member.userId}
                className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                    <User size={16} />
                  </div>
                  <span>{member.displayName || member.username}</span>
                </div>
                {/* 5. 调用修改后的 getRoleBadge 函数，并传入整个 member 对象 */}
                {getRoleBadge(member)}
              </li>
            ))}
          </ul>
        )}
      </div>
    </Modal>
  );
};

// Class Search Modal: search public classes by name, supports pagination and infinite scroll
const ClassSearchModal = ({ isOpen, onClose }) => {
  const [keyword, setKeyword] = useState("");
  const [debounceTimer, setDebounceTimer] = useState(null);
  const [results, setResults] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [joinStatus, setJoinStatus] = useState({});

  useEffect(() => {
    if (!isOpen) {
      setKeyword("");
      setResults([]);
      setPageInfo({ number: 0, totalPages: 0 });
      setError("");
    }
  }, [isOpen]);

  const doSearch = async (q, page = 0, append = false) => {
    if (!q || !q.trim()) {
      setResults([]);
      setPageInfo({ number: 0, totalPages: 0 });
      return;
    }
    setLoading(true);
    setError("");
    try {
      const res = await api.searchClasses(q, page, 20);
      if (res.error) {
        setError(res.error);
      } else {
        const items = res.classes || [];
        setResults((prev) => (append ? [...prev, ...items] : items));
        const p = res.page || {};
        setPageInfo({ number: p.number || 0, totalPages: p.totalPages || 0 });
      }
    } catch (err) {
      console.error('class search failed', err);
      setError(err.message || '搜索失败');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (v) => {
    setKeyword(v);
    if (debounceTimer) clearTimeout(debounceTimer);
    setDebounceTimer(
      setTimeout(() => {
        doSearch(v, 0, false);
      }, 350)
    );
  };

  const handleSubmit = (e) => {
    e?.preventDefault?.();
    if (debounceTimer) clearTimeout(debounceTimer);
    doSearch(keyword, 0, false);
  };

  const loadMore = async () => {
    if (loading) return;
    if (pageInfo.number + 1 >= (pageInfo.totalPages || 0)) return;
    const next = pageInfo.number + 1;
    await doSearch(keyword, next, true);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="搜索公开班级" size="lg">
      <form onSubmit={handleSubmit} className="mb-4">
        <div className="flex gap-2">
          <input
            type="text"
            className="flex-1 border px-3 py-2 rounded"
            placeholder="输入班级名称关键字，回车搜索"
            value={keyword}
            onChange={(e) => handleInputChange(e.target.value)}
          />
          <button className="px-4 py-2 bg-blue-600 text-white rounded" onClick={handleSubmit}>
            搜索
          </button>
        </div>
      </form>

      <div className="space-y-3 max-h-96 overflow-auto" onScroll={(e) => {
        const el = e.target;
        if (el.scrollTop + el.clientHeight >= el.scrollHeight - 16) {
          loadMore();
        }
      }}>
        {loading && results.length === 0 && <div className="text-center py-6">搜索中...</div>}
        {error && <div className="text-sm text-red-600">{error}</div>}
        {!loading && results.length === 0 && !error && (
          <div className="text-center text-gray-500 py-6">未找到相关班级</div>
        )}

        {results.map((c) => {
          const id = c.id || c.classId || c.uuid;
          const status = joinStatus[id] || {};
          return (
            <div key={id} className="p-3 border rounded bg-white mb-2">
              <div className="flex items-center justify-between">
                <div className="flex-1 mr-4">
                  <div className="font-medium text-gray-800">{c.name}</div>
                  <div className="text-xs text-gray-500">{c.description}</div>
                  <div className="text-xs text-gray-400 mt-1">创建者: {c.owner?.displayName || c.owner?.username}</div>
                </div>
                <div className="flex flex-col items-end">
                  <div className="text-sm text-gray-500 mb-2">{c.joinApprovalRequired ? '需审批' : '可直接加入'}</div>
                  <div>
                    {status.status === 'applied' ? (
                      <button className="px-3 py-1 bg-green-100 text-green-800 rounded text-xs" disabled>
                        已申请
                      </button>
                    ) : (
                      <button
                        className="px-3 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700 disabled:opacity-50"
                        onClick={async (e) => {
                          e.stopPropagation();
                          if (status.loading) return;
                          setJoinStatus(s => ({ ...s, [id]: { loading: true } }));
                          const res = await api.joinClassById(id);
                          if (res?.success) {
                            setJoinStatus(s => ({ ...s, [id]: { loading: false, status: 'applied', message: res.message } }));
                          } else {
                            setJoinStatus(s => ({ ...s, [id]: { loading: false, status: 'error', message: res?.message || '申请失败' } }));
                          }
                        }}
                        disabled={status.loading}
                      >
                        {status.loading ? '申请中...' : '申请加入'}
                      </button>
                    )}
                  </div>
                  {status.status === 'error' && <div className="text-xs text-red-500 mt-1">{status.message}</div>}
                </div>
              </div>
            </div>
          );
        })}

        {loading && results.length > 0 && <div className="text-center py-3">加载中...</div>}

        {pageInfo.totalPages > 0 && pageInfo.number + 1 < pageInfo.totalPages && (
          <div className="text-center py-3">
            <button onClick={loadMore} className="px-4 py-2 bg-gray-100 rounded">加载更多</button>
          </div>
        )}
      </div>
    </Modal>
  );
};
const ApprovalListModal = ({ isOpen, onClose, approvals, onProcess, loading }) => {
  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="待处理审批" size="lg">
      <div className="min-h-[400px] max-h-[70vh] flex flex-col">
        {loading ? (
          <div className="flex-1 flex justify-center items-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : approvals.length === 0 ? (
          <div className="flex-1 flex flex-col justify-center items-center text-center text-gray-500 py-10">
            <CheckCircle size={48} className="text-green-500 mb-4" />
            <p className="text-lg">全部处理完毕</p>
            <p className="text-sm">当前没有待处理的入班申请。</p>
          </div>
        ) : (
          <ul className="space-y-3 overflow-y-auto pr-2">
            {approvals.map((approval) => (
              <li key={approval.id} className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <p className="text-base font-semibold text-gray-800">
                      {approval.applicant.displayName || approval.applicant.username}
                    </p>
                    <p className="text-sm text-gray-500 mt-1">
                      申请加入: <span className="font-medium text-gray-600">{approval.classInfo.name}</span>
                    </p>
                    {approval.joinReason && (
                      <p className="text-sm text-gray-600 mt-2 bg-gray-50 p-2 rounded-md border">
                        申请理由: {approval.joinReason}
                      </p>
                    )}
                  </div>
                  <div className="flex flex-col sm:flex-row gap-2 ml-4">
                    <button
                      onClick={() => onProcess(approval, 'APPROVE')}
                      className="px-3 py-1.5 text-sm bg-green-500 text-white rounded-md hover:bg-green-600 flex items-center justify-center gap-1"
                    >
                      <CheckCircle size={14} />
                      同意
                    </button>
                    <button
                      onClick={() => onProcess(approval, 'REJECT')}
                      className="px-3 py-1.5 text-sm bg-red-500 text-white rounded-md hover:bg-red-600 flex items-center justify-center gap-1"
                    >
                      <XCircle size={14} />
                      拒绝
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Modal>
  );
};

// Date utilities
const dateUtils = {
  formatDate: (date) => {
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "long",
      day: "numeric",
    }).format(date);
  },

  formatDateTime: (date) => {
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  },

  getDaysInMonth: (year, month) => {
    return new Date(year, month + 1, 0).getDate();
  },

  getFirstDayOfMonth: (year, month) => {
    return new Date(year, month, 1).getDay();
  },

  isSameDay: (date1, date2) => {
    return (
      date1.getFullYear() === date2.getFullYear() &&
      date1.getMonth() === date2.getMonth() &&
      date1.getDate() === date2.getDate()
    );
  },

  formatDateString: (dateString) => {
    const date = new Date(dateString);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
      2,
      "0"
    )}-${String(date.getDate()).padStart(2, "0")}`;
  },

  toISOString: (date) => {
    return date.toISOString().slice(0, 16);
  },
};

// Format a local datetime-local string or Date into 'YYYY-MM-DDTHH:mm:ss' without timezone conversion
const formatLocalDateTime = (value) => {
  if (!value) return null;
  // if it's a Date object
  if (value instanceof Date) {
    const y = value.getFullYear();
    const m = String(value.getMonth() + 1).padStart(2, "0");
    const d = String(value.getDate()).padStart(2, "0");
    const hh = String(value.getHours()).padStart(2, "0");
    const mm = String(value.getMinutes()).padStart(2, "0");
    const ss = String(value.getSeconds()).padStart(2, "0");
    return `${y}-${m}-${d}T${hh}:${mm}:${ss}`;
  }

  // assume string like '2025-09-15T10:30' or '2025-09-15T10:30:00'
  const s = String(value);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(s)) {
    return s + ":00";
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(s)) {
    return s;
  }

  // fallback: try to parse as Date and extract local components
  const d = new Date(s);
  if (!isNaN(d.getTime())) {
    return formatLocalDateTime(d);
  }

  return null;
};

// Auth Context
const AuthContext = createContext();

const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("token") || "null");
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(false);

  const login = async (credentials) => {
    setLoading(true);
    try {
      const response = await api.login(credentials);
      setUser(response.user);
      setToken(response.token);
      localStorage.setItem("token", JSON.stringify(response.token));
      return response;
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    try {
      const response = await api.register(userData);
      setUser(response.user);
      setToken(response.token);
      localStorage.setItem("token", JSON.stringify(response.token));
      return response;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem("token");
  };

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        isAuthenticated,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

// Common Components
const Modal = ({ isOpen, onClose, title, children, size = "md" }) => {
  if (!isOpen) return null;

  const sizeClasses = {
    sm: "max-w-sm",
    md: "max-w-md",
    lg: "max-w-lg",
    xl: "max-w-xl",
  };

  // Backdrop is intentionally more opaque to make the modal clearly above the main UI
  const modalContent = (
    <div
      className="fixed inset-0 flex items-center justify-center"
      style={{ zIndex: 9998 }}
    >
      <div
        className="fixed inset-0 bg-black bg-opacity-80"
        style={{ zIndex: 9998 }}
        onClick={(e) => {
          e.stopPropagation();
          try {
            onClose();
          } catch (err) {
            console.error("modal onClose error", err);
          }
        }}
      />
      <div
        className={`relative bg-white rounded-xl p-6 w-full ${sizeClasses[size]} shadow-2xl max-h-[90vh] overflow-y-auto`}
        style={{ zIndex: 9999 }}
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-gray-800">{title}</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors p-1"
          >
            <X size={20} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );

  // Render modal into document.body to avoid stacking-context issues
  try {
    return createPortal(modalContent, document.body);
  } catch (err) {
    // fallback if document is not available
    console.warn("createPortal failed, rendering inline", err);
    return modalContent;
  }
};

const DateTimePicker = ({ value, onChange, label, required = false }) => {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <input
        type="datetime-local"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
        required={required}
      />
    </div>
  );
};

const ContextMenu = ({ isOpen, onClose, x, y, children }) => {
  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} />
      <div
        className="fixed bg-white border border-gray-200 rounded-lg shadow-lg z-50 py-1 min-w-32"
        style={{ left: x, top: y }}
      >
        {children}
      </div>
    </>
  );
};

// Inline component to resend verification email per backend spec
const ResendVerificationInline = ({ email }) => {
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState(null); // { type: 'success'|'error', message }
  const [localEmail, setLocalEmail] = useState(email || "");

  useEffect(() => {
    if (email) setLocalEmail(email);
  }, [email]);

  const handleResend = async (e) => {
    e?.preventDefault?.();
    if (!localEmail) return setStatus({ type: 'error', message: '请输入邮箱' });
    setLoading(true);
    setStatus(null);
    try {
      const res = await fetch(`${API_BASE}/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: localEmail }),
      });
      const payload = await res.json().catch(() => null);
      if (res.status === 200) {
        setStatus({ type: 'success', message: payload?.message || '新的验证邮件已发送，请检查您的收件箱。' });
      } else if (res.status === 404) {
        setStatus({ type: 'error', message: payload?.message || '该邮箱未注册' });
      } else if (res.status === 400) {
        setStatus({ type: 'error', message: payload?.message || '该邮箱已经验证，请直接登录' });
      } else {
        setStatus({ type: 'error', message: payload?.message || `发送失败（${res.status}）` });
      }
    } catch (err) {
      console.error('Resend verification error', err);
      setStatus({ type: 'error', message: '网络错误，请稍后重试' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center gap-2">
      <form onSubmit={handleResend} className="flex items-center gap-2">
        <input
          type="email"
          className="border px-3 py-2 rounded-l"
          placeholder="邮箱地址"
          value={localEmail}
          onChange={(e) => setLocalEmail(e.target.value)}
        />
        <button
          type="submit"
          disabled={loading}
          className="px-3 py-2 bg-yellow-500 text-white rounded-r hover:bg-yellow-600 disabled:opacity-50"
        >
          {loading ? '发送中...' : '重新发送验证邮件'}
        </button>
      </form>
      {status && (
        <div className={status.type === 'error' ? 'text-red-600 text-sm' : 'text-green-600 text-sm'}>
          {status.message}
        </div>
      )}
    </div>
  );
};

// Verify Email landing page component
const VerifyEmailPage = () => {
  const [status, setStatus] = useState('loading'); // loading | success | error
  const [message, setMessage] = useState('');
  const [emailInput, setEmailInput] = useState('');

  useEffect(() => {
    // read token from query param
    try {
      const params = new URLSearchParams(window.location.search);
      const token = params.get('token');
      if (!token) {
        setStatus('error');
        setMessage('缺少 token，请检查链接');
        return;
      }

      (async () => {
        try {
          const res = await fetch(`${API_BASE}/auth/verify-email?token=${encodeURIComponent(token)}`);
          const payload = await res.json().catch(() => null);
          if (res.ok) {
            setStatus('success');
            setMessage(payload?.message || '邮箱验证成功，您现在可以登录。');
          } else {
            setStatus('error');
            setMessage(payload?.message || `验证失败（${res.status}）`);
          }
        } catch (err) {
          console.error('verify email failed', err);
          setStatus('error');
          setMessage('网络错误，无法验证邮箱');
        }
      })();
    } catch (err) {
      console.error(err);
      setStatus('error');
      setMessage('无法读取链接参数');
    }
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-white to-gray-50">
      <div className="bg-white p-8 rounded-2xl shadow-2xl w-full max-w-xl text-center">
        <h2 className="text-2xl font-bold mb-4">邮箱验证</h2>
        {status === 'loading' && <div className="text-gray-600">验证中，请稍候...</div>}
        {status === 'success' && (
          <div className="space-y-4">
            <div className="text-green-600 font-medium">{message}</div>
            <div>
              <a href="/" className="px-4 py-2 bg-blue-600 text-white rounded">前往登录</a>
            </div>
          </div>
        )}
        {status === 'error' && (
          <div className="space-y-4">
            <div className="text-red-600">{message}</div>
            <div className="text-sm text-gray-600">如果需要，您也可以重新发送验证邮件：</div>
            <div className="mt-3">
              <div className="flex items-center justify-center">
                <input
                  className="border px-3 py-2 rounded-l w-64"
                  placeholder="请输入要重新发送验证的邮箱"
                  value={emailInput}
                  onChange={(e) => setEmailInput(e.target.value)}
                />
                <div className="rounded-r">
                  <ResendVerificationInline email={emailInput} />
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// Auth Components
const LoginForm = ({ onSwitchToRegister }) => {
  const { login, loading } = useAuth();
  const [formData, setFormData] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [unverifiedEmail, setUnverifiedEmail] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setError("");
      setUnverifiedEmail(null);
      await login(formData);
    } catch (e) {
      console.error(e);
      // detect unverified-email situation from backend
      const status = e?.status;
      const srvMsg = (e?.data?.message || e?.message || "").toString().toLowerCase();
      const isNotEnabled =
        status === 401 &&
        (srvMsg.includes("not enabled") || srvMsg.includes("not verified") || srvMsg.includes("未验证") || srvMsg.includes("disabled") || srvMsg.includes("not active") || srvMsg.includes("user is not enabled"));

      if (isNotEnabled) {
        setUnverifiedEmail(formData.username || formData.email || null);
        setError("登录失败：您的邮箱尚未验证。请检查您的邮箱以完成验证。");
      } else {
        setError("登录失败，请检查用户名和密码");
      }
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="bg-white p-8 rounded-2xl shadow-2xl w-full max-w-md">
        <h2 className="text-3xl font-bold text-center text-gray-800 mb-8">
          登录
        </h2>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              用户名
            </label>
            <input
              type="text"
              value={formData.username}
              onChange={(e) =>
                setFormData({ ...formData, username: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
              placeholder="请输入用户名"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              密码
            </label>
            <input
              type="password"
              value={formData.password}
              onChange={(e) =>
                setFormData({ ...formData, password: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
              placeholder="请输入密码"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? "登录中..." : "登录"}
          </button>
        </form>

        <p className="text-center mt-6 text-gray-600">
          还没有账号？
          <button
            onClick={onSwitchToRegister}
            className="text-blue-600 hover:text-blue-700 font-medium ml-1"
          >
            立即注册
          </button>
        </p>
        {unverifiedEmail && (
          <div className="mt-4 text-center">
            <div className="text-sm text-red-600">{error}</div>
            <div className="mt-3">
              <ResendVerificationInline email={unverifiedEmail} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const RegisterForm = ({ onSwitchToLogin }) => {
  const { loading } = useAuth();
  const [formData, setFormData] = useState({
    name: "",
    username: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [registeredEmail, setRegisteredEmail] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (formData.password !== formData.confirmPassword) {
      setError("两次输入的密码不一致");
      return;
    }

    if (formData.password.length < 6) {
      setError("密码长度至少6位");
      return;
    }

    if (!formData.email || !formData.email.includes("@")) {
      setError("请输入有效的邮箱地址");
      return;
    }

    if (!formData.phone || formData.phone.length < 11) {
      setError("请输入有效的手机号码");
      return;
    }

    try {
      // call backend register directly; don't auto-login here
      const res = await api.register(formData);
      // show verification view using the email the user provided
      setRegisteredEmail(formData.email || res?.user?.email || "");
    } catch (e) {
      console.error(e);
      setError("注册失败，请重试");
    }
  };

  if (registeredEmail) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-50 to-emerald-100">
        <div className="bg-white p-8 rounded-2xl shadow-2xl w-full max-w-md text-center">
          <h2 className="text-2xl font-bold text-gray-800 mb-4">验证您的邮箱</h2>
          <p className="text-sm text-gray-700">
            注册成功！一封包含激活链接的邮件已发送至您的邮箱 <span className="font-mono">{registeredEmail}</span>。
            请检查您的收件箱（包括垃圾邮件文件夹），并点击链接以完成注册。
          </p>
          <div className="mt-6">
            <ResendVerificationInline email={registeredEmail} />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-50 to-emerald-100">
      <div className="bg-white p-8 rounded-2xl shadow-2xl w-full max-w-md">
        <h2 className="text-3xl font-bold text-center text-gray-800 mb-8">
          注册
        </h2>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              姓名
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent transition-colors"
              placeholder="请输入姓名"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              用户名
            </label>
            <input
              type="text"
              value={formData.username}
              onChange={(e) =>
                setFormData({ ...formData, username: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent transition-colors"
              placeholder="请输入用户名"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              邮箱
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) =>
                setFormData({ ...formData, email: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent transition-colors"
              placeholder="请输入邮箱地址"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              手机号
            </label>
            <input
              type="tel"
              value={formData.phone}
              onChange={(e) =>
                setFormData({ ...formData, phone: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent transition-colors"
              placeholder="请输入手机号码"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              密码
            </label>
            <input
              type="password"
              value={formData.password}
              onChange={(e) =>
                setFormData({ ...formData, password: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent transition-colors"
              placeholder="请输入密码"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              确认密码
            </label>
            <input
              type="password"
              value={formData.confirmPassword}
              onChange={(e) =>
                setFormData({ ...formData, confirmPassword: e.target.value })
              }
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent transition-colors"
              placeholder="请再次输入密码"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-green-600 text-white py-3 rounded-lg font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? "注册中..." : "注册"}
          </button>
        </form>

        <p className="text-center mt-6 text-gray-600">
          已有账号？
          <button
            onClick={onSwitchToLogin}
            className="text-green-600 hover:text-green-700 font-medium ml-1"
          >
            立即登录
          </button>
        </p>
      </div>
    </div>
  );
};

// Task Components
const TaskMarker = ({ task, type }) => {
  const isCreated = type === "created";

  if (isCreated) {
    // small colored circle
    return (
      <div title={`${task.courseName} - ${task.title}`} className="mb-1">
        <span
          className="inline-block w-2 h-2 rounded-full"
          style={{ backgroundColor: task.courseColor || "#6b7280" }}
          aria-hidden="true"
        />
      </div>
    );
  }

  // due task -> small red square
  return (
    <div title={`截止: ${task.courseName} - ${task.title}`} className="mb-1">
      <span
        className="inline-block w-2 h-2"
        style={{ backgroundColor: "#ef4444" }}
        aria-hidden="true"
      />
    </div>
  );
};

const TaskEditor = ({ task, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    title: task?.title || "",
    description: task?.description || "",
    dueDate: task?.dueDate ? dateUtils.toISOString(new Date(task.dueDate)) : "",
    status: task?.status || "pending",
  });
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave({ ...task, ...formData });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-gray-50 border-2 border-blue-200 rounded-lg p-4 mb-3">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            任务标题 *
          </label>
          <input
            type="text"
            value={formData.title}
            onChange={(e) =>
              setFormData({ ...formData, title: e.target.value })
            }
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            描述
          </label>
          <textarea
            value={formData.description}
            onChange={(e) =>
              setFormData({ ...formData, description: e.target.value })
            }
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            rows="3"
            placeholder="任务详细描述..."
          />
        </div>

        <DateTimePicker
          value={formData.dueDate}
          onChange={(value) => setFormData({ ...formData, dueDate: value })}
          label="截止时间"
          required
        />

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            状态
          </label>
          <select
            value={formData.status}
            onChange={(e) =>
              setFormData({ ...formData, status: e.target.value })
            }
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="pending">待办</option>
            <option value="in_progress">进行中</option>
            <option value="completed">已完成</option>
          </select>
        </div>

        <div className="flex gap-2 pt-2">
          <button
            type="submit"
            disabled={saving}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center gap-2"
          >
            {saving ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <Check size={16} />
            )}
            保存
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
          >
            取消
          </button>
        </div>
      </form>
    </div>
  );
};

// Task Edit Modal (full featured editor per requirements)
const TaskEditModal = ({ isOpen, onClose, task, currentUser, onSaved }) => {
  const [local, setLocal] = useState(null);
  const [editingField, setEditingField] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isOpen && task) {
      // normalize initial local state
      const initial = {
        ...task,
        // official deadline
        deadline: task.deadline || task.dueDate || null,
        createdDate: task.createdDate || task.createdAt || null,
        // normalize status to local values used by UI
        status: (function () {
          const s = (
            task.personalStatus ||
            task.status ||
            task.userTaskStatus ||
            ""
          ).toString();
          if (!s) return "pending";
          const up = s.toUpperCase();
          if (up.includes("IN_PROGRESS")) return "in_progress";
          if (up === "DONE" || up === "COMPLETED") return "completed";
          return "pending";
        })(),
        personalDeadline:
          task.personalDeadline || task.personalDeadline || null,
        personalNotes: task.personalNotes || task.personalNotes || "",
      };

      setLocal(initial);
      setEditingField(null);
      setError("");
    }
  }, [isOpen, task]);

  if (!isOpen || !task) return null;

  // determine if user is admin for this class task
  const isClassTask =
    (task.type || task.taskType || "").toLowerCase() === "class" ||
    !!task.classId;
  const isAdmin =
    currentUser &&
    (currentUser.role === "ADMIN" ||
      currentUser.role === "TEACHER" ||
      task?.isCreator);

  const mapLocalToServerStatus = (localStatus) => {
    const m = {
      // backend enum values: TODO, IN_PROGRESS, DONE
      pending: "TODO",
      in_progress: "IN_PROGRESS",
      completed: "DONE",
    };
    if (!localStatus) return "TODO";
    return m[localStatus] || localStatus.toString().toUpperCase();
  };

  const handleSave = async () => {
    setSaving(true);
    setError("");
    try {
      // Debug: dump local state to help diagnose incorrect personalDeadline being sent
      try {
        console.debug(
          "[TaskEditModal] DUMP before save local:",
          JSON.parse(JSON.stringify(local))
        );
      } catch (err) {
        console.debug(
          "[TaskEditModal] DUMP before save local (raw), stringify failed:",
          local,
          "error:",
          err
        );
      }
      if (!isClassTask) {
        // personal task -> update official task via PUT /api/tasks/{taskId}
        const sentDeadline = formatLocalDateTime(local.deadline);
        console.debug(
          "[TaskEditModal] personal update payload deadline local:",
          local.deadline,
          "sent:",
          sentDeadline
        );
        const payload = {
          title: local.title,
          description: local.description,
          courseName: local.courseName,
          // send local datetime string without timezone conversion
          deadline: sentDeadline,
        };
        await api.updateTask(task.id, payload);
        // api.updateTask throws on error
      } else {
        if (isAdmin) {
          // admin edits official fields of class task
          const sentDeadlineAdmin = formatLocalDateTime(local.deadline);
          console.debug(
            "[TaskEditModal] admin update payload deadline local:",
            local.deadline,
            "sent:",
            sentDeadlineAdmin
          );
          const payload = {
            title: local.title,
            description: local.description,
            courseName: local.courseName,
            deadline: sentDeadlineAdmin,
          };
          await api.updateTask(task.id, payload);
        } else {
          // regular class member updates personal plan via /tasks/{id}/status
          const mappedStatus = mapLocalToServerStatus(
            local.status || task.personalStatus || task.status
          );
          const sentPersonalDeadline = local.personalDeadline
            ? formatLocalDateTime(local.personalDeadline)
            : null;
          console.debug(
            "[TaskEditModal] member update payload status:",
            mappedStatus,
            "personalDeadline local:",
            local.personalDeadline,
            "sent:",
            sentPersonalDeadline
          );
          const payload = {
            status: mappedStatus,
            personalDeadline: sentPersonalDeadline,
            personalNotes: local.personalNotes || null,
          };
          const res = await api.updateTaskStatus(task.id, payload, true);
          if (!res.success) {
            console.error("updateTaskStatus failed raw:", res.raw);
            throw new Error(
              res.message
                ? res.message +
                  (res.raw ? " | raw: " + JSON.stringify(res.raw) : "")
                : "保存失败"
            );
          }
        }
      }

      if (onSaved) onSaved(task.id);
      onClose();
    } catch (err) {
      console.error("Failed to save task in modal", err);
      setError(err.message || "保存失败");
    } finally {
      setSaving(false);
    }
  };

  // simplified year/month/day selectors to emulate wheels
  const renderDatePicker = () => {
    // when editing deadline we show/select based on local.deadline or local.dueDate
    const current = local?.deadline
      ? new Date(local.deadline)
      : local?.dueDate
      ? new Date(local.dueDate)
      : new Date();
    const years = Array.from(
      { length: 10 },
      (_, i) => current.getFullYear() - 5 + i
    );
    const months = Array.from({ length: 12 }, (_, i) => i + 1);
    const days = Array.from({ length: 31 }, (_, i) => i + 1);

    const updateDate = (year, month, day) => {
      const d = new Date(
        year,
        month - 1,
        day,
        current.getHours(),
        current.getMinutes(),
        0
      );
      const iso = d.toISOString().slice(0, 19);
      // keep both official deadline and displayed dueDate in sync
      // If this is a class task and the user is not an admin, they are editing their personal plan
      // so also update personalDeadline to the selected value to ensure the payload matches UI.
      if (isClassTask && !isAdmin) {
        setLocal((prev) => ({
          ...prev,
          dueDate: iso,
          deadline: iso,
          personalDeadline: iso,
        }));
      } else {
        setLocal((prev) => ({ ...prev, dueDate: iso, deadline: iso }));
      }
    };

    const selYear = current.getFullYear();
    const selMonth = current.getMonth() + 1;
    const selDay = current.getDate();

    return (
      <div className="flex gap-2">
        <select
          className="border p-2 rounded"
          defaultValue={selYear}
          onChange={(e) => updateDate(Number(e.target.value), selMonth, selDay)}
        >
          {years.map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>
        <select
          className="border p-2 rounded"
          defaultValue={selMonth}
          onChange={(e) => updateDate(selYear, Number(e.target.value), selDay)}
        >
          {months.map((m) => (
            <option key={m} value={m}>
              {String(m).padStart(2, "0")}
            </option>
          ))}
        </select>
        <select
          className="border p-2 rounded"
          defaultValue={selDay}
          onChange={(e) =>
            updateDate(selYear, selMonth, Number(e.target.value))
          }
        >
          {days.map((d) => (
            <option key={d} value={d}>
              {String(d).padStart(2, "0")}
            </option>
          ))}
        </select>
      </div>
    );
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="编辑任务" size="md">
      <div className="space-y-4">
        <div>
          <div className="text-sm text-gray-500">标题</div>
          {editingField === "title" ? (
            <input
              value={local?.title || ""}
              onChange={(e) =>
                setLocal((prev) => ({ ...prev, title: e.target.value }))
              }
              className="w-full border p-2 rounded"
            />
          ) : (
            <div
              onClick={() => {
                if (!isClassTask || isAdmin) setEditingField("title");
              }}
              className="p-2"
            >
              {local?.title || <span className="text-gray-400">（空）</span>}
            </div>
          )}
        </div>

        <div>
          <div className="text-sm text-gray-500">描述</div>
          {editingField === "description" ? (
            <textarea
              value={local?.description || ""}
              onChange={(e) =>
                setLocal((prev) => ({ ...prev, description: e.target.value }))
              }
              className="w-full border p-2 rounded"
              rows={4}
            />
          ) : (
            <div
              onClick={() => {
                if (!isClassTask || isAdmin) setEditingField("description");
              }}
              className="p-2"
            >
              {local?.description || (
                <span className="text-gray-400">（无描述）</span>
              )}
            </div>
          )}
        </div>

        <div>
          <div className="text-sm text-gray-500">课程</div>
          {editingField === "courseName" ? (
            <input
              value={local?.courseName || ""}
              onChange={(e) =>
                setLocal((prev) => ({ ...prev, courseName: e.target.value }))
              }
              className="w-full border p-2 rounded"
            />
          ) : (
            <div
              onClick={() => {
                if (!isClassTask || isAdmin) setEditingField("courseName");
              }}
              className="p-2"
            >
              {local?.courseName || (
                <span className="text-gray-400">（未指定）</span>
              )}
            </div>
          )}
        </div>

        {/* createdDate is immutable; do not allow editing here. show as read-only tag */}
        <div>
          <div className="text-sm text-gray-500">创建日期</div>
          <div className="p-2 text-gray-700">
            {local?.createdDate ? (
              String(local.createdDate).replace("T", " ").slice(0, 16)
            ) : (
              <span className="text-gray-400">（未设置）</span>
            )}
          </div>
        </div>

        <div>
          <div className="text-sm text-gray-500">截止日期</div>
          {editingField === "deadline" ? (
            renderDatePicker()
          ) : (
            <div onClick={() => setEditingField("deadline")} className="p-2">
              {local?.deadline || local?.dueDate ? (
                (local.deadline || local.dueDate).replace("T", " ").slice(0, 16)
              ) : (
                <span className="text-gray-400">无截止日期</span>
              )}
            </div>
          )}
        </div>

        {error && <div className="text-sm text-red-600">{error}</div>}

        <div className="flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 bg-gray-100 rounded">
            取消
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 bg-blue-600 text-white rounded"
          >
            {saving ? "保存中..." : "确定"}
          </button>
        </div>
      </div>
    </Modal>
  );
};

// Task Create Modal - reuse similar structure/styling as TaskEditModal
const TaskCreateModal = ({
  isOpen,
  onClose,
  defaultDeadline,
  classId = null,
  onCreated,
}) => {
  const [form, setForm] = useState({
    title: "",
    description: "",
    courseName: "",
    deadline: defaultDeadline ? defaultDeadline() : "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isOpen) {
      setForm({
        title: "",
        description: "",
        courseName: "",
        deadline: defaultDeadline ? defaultDeadline() : "",
      });
      setError("");
    }
  }, [isOpen, defaultDeadline]);

  const handleCreate = async () => {
    setSaving(true);
    setError("");
    try {
      const payload = {
        title: form.title,
        description: form.description,
        courseName: form.courseName,
        deadline: formatLocalDateTime(form.deadline) || null,
      };

      // choose endpoint based on presence of classId
      if (classId) {
        await api.createTask({ ...payload, classId });
      } else {
        await api.createTask(payload);
      }

      if (onCreated) onCreated();
      onClose();
    } catch (err) {
      console.error("Failed to create task", err);
      setError(err.message || "创建失败");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={classId ? "新建班级任务" : "新建个人任务"}
    >
      <div className="space-y-4">
        <div>
          <div className="text-sm text-gray-500">标题</div>
          <input
            value={form.title}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, title: e.target.value }))
            }
            className="w-full border p-2 rounded"
            placeholder="任务标题"
          />
        </div>

        <div>
          <div className="text-sm text-gray-500">描述</div>
          <textarea
            value={form.description}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, description: e.target.value }))
            }
            className="w-full border p-2 rounded"
            rows={4}
            placeholder="任务描述"
          />
        </div>

        <div>
          <div className="text-sm text-gray-500">课程</div>
          <input
            value={form.courseName}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, courseName: e.target.value }))
            }
            className="w-full border p-2 rounded"
            placeholder="课程名称"
          />
        </div>

        <DateTimePicker
          value={form.deadline}
          onChange={(v) => setForm((prev) => ({ ...prev, deadline: v }))}
          label="截止时间"
          required
        />

        {error && <div className="text-sm text-red-600">{error}</div>}

        <div className="flex justify-end gap-2">
          <button onClick={onClose} className="px-4 py-2 bg-gray-100 rounded">
            取消
          </button>
          <button
            onClick={handleCreate}
            disabled={saving}
            className="px-4 py-2 bg-blue-600 text-white rounded"
          >
            {saving ? "创建中..." : "确定"}
          </button>
        </div>
      </div>
    </Modal>
  );
};

// Task Editor Context - make editor globally openable from anywhere
const TaskEditorContext = createContext({ open: () => {}, close: () => {} });

const TaskEditorProvider = ({ children }) => {
  const [modalTask, setModalTask] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  const savedCallbackRef = React.useRef(null);
  const { user } = useAuth();

  const open = (task, onSaved) => {
    setModalTask(task);
    savedCallbackRef.current = onSaved || null;
    setIsOpen(true);
  };

  const close = () => {
    setIsOpen(false);
    setModalTask(null);
    savedCallbackRef.current = null;
  };

  return (
    <TaskEditorContext.Provider value={{ open, close }}>
      {children}
      <TaskEditModal
        isOpen={isOpen}
        onClose={close}
        task={modalTask}
        currentUser={user}
        onSaved={async (taskId) => {
          try {
            if (savedCallbackRef.current)
              await savedCallbackRef.current(taskId);
            // notify global listeners (e.g., CalendarView) that a task changed
            try {
              window.dispatchEvent(
                new CustomEvent("task-updated", { detail: { taskId } })
              );
            } catch (e) {
              console.warn("dispatch task-updated failed", e);
            }
          } finally {
            // always close afterwards
            close();
          }
        }}
      />
    </TaskEditorContext.Provider>
  );
};

const TaskCard = ({ task, onEdit, onDelete, onNavigateToDeadline }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [contextMenu, setContextMenu] = useState({ isOpen: false, x: 0, y: 0 });

  const handleContextMenu = (e) => {
    e.preventDefault();
    setContextMenu({
      isOpen: true,
      x: e.clientX,
      y: e.clientY,
    });
  };

  const closeContextMenu = () => {
    setContextMenu({ isOpen: false, x: 0, y: 0 });
  };

  const getStatusBadge = () => {
    const statusConfig = {
      pending: { text: "待办", color: "bg-yellow-100 text-yellow-800" },
      in_progress: { text: "进行中", color: "bg-blue-100 text-blue-800" },
      completed: { text: "已完成", color: "bg-green-100 text-green-800" },
    };
    const config = statusConfig[task.status] || statusConfig.pending;
    return (
      <span
        className={`px-2 py-1 rounded-full text-xs font-medium ${config.color}`}
      >
        {config.text}
      </span>
    );
  };

  const isOverdue =
    new Date(task.dueDate) < new Date() && task.status !== "completed";

  return (
    <>
      <div
        className={`bg-white border rounded-lg p-4 mb-3 hover:shadow-md transition-all duration-200 cursor-pointer ${
          isOverdue ? "border-red-300 bg-red-50" : "border-gray-200"
        }`}
        onContextMenu={handleContextMenu}
        onClick={() => onEdit && onEdit(task)}
      >
        <div className="flex justify-between items-start mb-2">
          <div className="flex items-center gap-2 flex-wrap">
            <div
              className="w-3 h-3 rounded"
              style={{ backgroundColor: task.courseColor }}
            />
            <span className="text-sm font-medium text-gray-600">
              {task.courseName}
            </span>
            <span className="text-base font-semibold text-gray-800">
              {task.title}
            </span>
            {getStatusBadge()}
            {isOverdue && (
              <span className="px-2 py-1 bg-red-100 text-red-800 rounded-full text-xs font-medium">
                已逾期
              </span>
            )}
          </div>
        </div>

        <div className="text-sm text-gray-600 mb-2">
          <div className="flex items-center gap-4">
            <span>
              📅 截止: {dateUtils.formatDateTime(new Date(task.dueDate))}
            </span>
            <span>📝 创建: {dateUtils.formatDateString(task.createdDate)}</span>
          </div>
        </div>

        {task.description && (
          <div className="mt-2">
            <p
              className={`text-gray-600 text-sm ${
                !isExpanded ? "line-clamp-2" : ""
              }`}
            >
              {task.description}
            </p>
            <button
              onClick={(e) => {
                e.stopPropagation();
                setIsExpanded(!isExpanded);
              }}
              className="text-blue-600 text-sm hover:text-blue-700 mt-1"
            >
              {isExpanded ? "收起" : "展开"}
            </button>
          </div>
        )}
      </div>

      <ContextMenu
        isOpen={contextMenu.isOpen}
        onClose={closeContextMenu}
        x={contextMenu.x}
        y={contextMenu.y}
      >
        <button
          onClick={() => {
            onEdit(task);
            closeContextMenu();
          }}
          className="w-full px-4 py-2 text-left hover:bg-gray-50 text-sm flex items-center gap-2"
        >
          <Edit3 size={14} />
          修改
        </button>
        <button
          onClick={() => {
            onNavigateToDeadline(task);
            closeContextMenu();
          }}
          className="w-full px-4 py-2 text-left hover:bg-gray-50 text-sm flex items-center gap-2"
        >
          <CalendarIcon size={14} />
          跳转到截止日
        </button>
        <button
          onClick={() => {
            onDelete(task.id);
            closeContextMenu();
          }}
          className="w-full px-4 py-2 text-left hover:bg-gray-50 text-sm text-red-600 flex items-center gap-2"
        >
          <X size={14} />
          删除
        </button>
      </ContextMenu>
    </>
  );
};

// Calendar Components
const CalendarDay = ({ date, isToday, isCurrentMonth, tasks, onDateClick }) => {
  const [showTooltip, setShowTooltip] = useState(false);

  const dayTasks = useMemo(() => {
    const dateStr = dateUtils.formatDateString(date);
    return {
      created: tasks.filter((task) => task.createdDate === dateStr),
      due: tasks.filter((task) => (task?.dueDate || "").startsWith(dateStr)),
    };
  }, [date, tasks]);

  const totalTasks = dayTasks.created.length + dayTasks.due.length;

  return (
    <div
      className={`
                relative h-32 border border-gray-200 p-2 cursor-pointer transition-all duration-200
                hover:bg-gray-50 hover:shadow-sm
                ${
                  isToday
                    ? "bg-blue-50 border-blue-300 ring-1 ring-blue-200"
                    : ""
                }
                ${!isCurrentMonth ? "text-gray-400 bg-gray-50" : ""}
            `}
      onClick={(e) => {
        e.stopPropagation();
        onDateClick && onDateClick(date);
      }}
      onMouseEnter={() => setShowTooltip(true)}
      onMouseLeave={() => setShowTooltip(false)}
    >
      <div className={`text-sm font-medium ${isToday ? "text-blue-600" : ""}`}>
        {date.getDate()}
      </div>

      <div className="mt-1 space-y-1 overflow-hidden">
        {dayTasks.created.slice(0, 3).map((task) => (
          <TaskMarker key={`created-${task.id}`} task={task} type="created" />
        ))}
        {dayTasks.due.slice(0, 3 - dayTasks.created.length).map((task) => (
          <TaskMarker key={`due-${task.id}`} task={task} type="due" />
        ))}

        {totalTasks > 3 && (
          <div className="text-xs text-gray-500 font-medium">
            +{totalTasks - 3}
          </div>
        )}
      </div>

      {showTooltip && totalTasks > 0 && (
        <div className="absolute z-10 bg-white border border-gray-200 rounded-lg shadow-xl p-3 top-full left-0 mt-1 min-w-64 max-w-80">
          <div className="space-y-2">
            <h4 className="font-medium text-gray-800 text-sm">
              {dateUtils.formatDate(date)}
            </h4>
            {dayTasks.created.map((task) => (
              <div
                key={`tooltip-created-${task.id}`}
                className="text-xs flex items-center gap-2"
              >
                <span className="w-2 h-2 bg-lime-500 rounded-full flex-shrink-0" />
                <span className="text-gray-600">创建:</span>
                <span className="font-medium">{task.courseName}</span>
                <span className="text-gray-800">{task.title}</span>
              </div>
            ))}
            {dayTasks.due.map((task) => (
              <div
                key={`tooltip-due-${task.id}`}
                className="text-xs flex items-center gap-2"
              >
                <div
                  className="w-2 h-2 rounded-sm flex-shrink-0"
                  style={{ backgroundColor: task.courseColor }}
                />
                <span className="text-gray-600">截止:</span>
                <span className="font-medium">{task.courseName}</span>
                <span className="text-gray-800">{task.title}</span>
              </div>
            ))}
          </div>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDateClick(date);
            }}
            className="mt-3 text-xs text-blue-600 hover:text-blue-700 font-medium"
          >
            查看详情 →
          </button>
        </div>
      )}
    </div>
  );
};

const CalendarView = ({ onDateSelect }) => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const daysInMonth = dateUtils.getDaysInMonth(year, month);
  const firstDay = dateUtils.getFirstDayOfMonth(year, month);
  const today = new Date();

  // Load calendar data
  const loadCalendarData = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.getCalendarData(year, month + 1);
      setTasks(data.tasks);
    } catch (error) {
      console.error("Failed to load calendar data:", error);
    } finally {
      setLoading(false);
    }
  }, [year, month]);

  useEffect(() => {
    loadCalendarData();
    // listen for global task updates so the calendar can refresh after edits
    const onTaskUpdated = () => {
      try {
        loadCalendarData();
      } catch (err) {
        console.error("refresh on task-updated failed", err);
      }
    };
    window.addEventListener("task-updated", onTaskUpdated);

    return () => {
      window.removeEventListener("task-updated", onTaskUpdated);
    };
  }, [loadCalendarData]);

  const navigateMonth = (direction) => {
    setCurrentDate(new Date(year, month + direction, 1));
  };

  const renderCalendarDays = () => {
    const days = [];
    const totalCells = Math.ceil((daysInMonth + firstDay) / 7) * 7;

    for (let i = 0; i < totalCells; i++) {
      const dayNumber = i - firstDay + 1;
      let date, isCurrentMonth;

      if (i < firstDay) {
        // Previous month
        const prevMonth = month === 0 ? 11 : month - 1;
        const prevYear = month === 0 ? year - 1 : year;
        const prevMonthDays = dateUtils.getDaysInMonth(prevYear, prevMonth);
        date = new Date(prevYear, prevMonth, prevMonthDays - firstDay + i + 1);
        isCurrentMonth = false;
      } else if (dayNumber <= daysInMonth) {
        // Current month
        date = new Date(year, month, dayNumber);
        isCurrentMonth = true;
      } else {
        // Next month
        const nextMonth = month === 11 ? 0 : month + 1;
        const nextYear = month === 11 ? year + 1 : year;
        date = new Date(nextYear, nextMonth, dayNumber - daysInMonth);
        isCurrentMonth = false;
      }

      const isToday = dateUtils.isSameDay(date, today);

      days.push(
        <CalendarDay
          key={`${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`}
          date={date}
          isToday={isToday}
          isCurrentMonth={isCurrentMonth}
          tasks={tasks}
          onDateClick={onDateSelect}
        />
      );
    }

    return days;
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-gray-800">
          {year}年{month + 1}月
        </h2>
        <div className="flex gap-2">
          <button
            onClick={() => navigateMonth(-1)}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <ChevronLeft size={20} />
          </button>
          <button
            onClick={() => setCurrentDate(new Date())}
            className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            今天
          </button>
          <button
            onClick={() => navigateMonth(1)}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <ChevronRight size={20} />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-7 gap-px mb-2">
        {["日", "一", "二", "三", "四", "五", "六"].map((day) => (
          <div
            key={day}
            className="h-10 flex items-center justify-center text-sm font-medium text-gray-500"
          >
            {day}
          </div>
        ))}
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-96">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <div className="grid grid-cols-7 gap-px bg-gray-200 rounded-lg overflow-visible">
          {renderCalendarDays()}
        </div>
      )}
    </div>
  );
};

const TaskDetail = ({ selectedDate, onBack, onNavigateToDate }) => {
  const [tasks, setTasks] = useState([]);
  const [editingTask, setEditingTask] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [loading, setLoading] = useState(false);
  const taskEditor = useContext(TaskEditorContext);

  const selectedTasks = useMemo(() => {
    const dateStr = dateUtils.formatDateString(selectedDate);
    return {
      created: tasks.filter(
        (task) =>
          (task?.createdDate || "").startsWith(dateStr) ||
          task?.createdDate === dateStr
      ),
      due: tasks.filter((task) => (task?.dueDate || "").startsWith(dateStr)),
    };
  }, [selectedDate, tasks]);

  useEffect(() => {
    const loadTasks = async () => {
      setLoading(true);
      try {
        const data = await api.getCalendarData(
          selectedDate.getFullYear(),
          selectedDate.getMonth() + 1
        );
        setTasks(data.tasks);
      } catch (error) {
        console.error("Failed to load tasks:", error);
      } finally {
        setLoading(false);
      }
    };

    loadTasks();
  }, [selectedDate]);

  const handleSaveTask = async (updatedTask) => {
    try {
      if (!updatedTask.id) {
        // create
        const res = await api.createTask({
          ...updatedTask,
          createdDate: dateUtils.formatDateString(selectedDate),
        });
        if (res.success && res.task) {
          setTasks((prev) => [res.task, ...prev]);
          setEditingTask(null);
          return;
        }
        throw new Error("创建失败");
      }
      // When updating from the TaskDetail inline editor, ensure deadline is formatted as local datetime
      const payload = { ...updatedTask };
      if (updatedTask.deadline)
        payload.deadline = formatLocalDateTime(updatedTask.deadline);
      console.debug("[TaskDetail] updateTask payload:", payload);
      await api.updateTask(updatedTask.id, payload);
      setTasks((prev) =>
        prev.map((task) => (task.id === updatedTask.id ? updatedTask : task))
      );
      setEditingTask(null);
    } catch (error) {
      console.error("Failed to save task:", error);
      alert("保存失败，请重试");
    }
  };

  const handleDeleteTask = async (taskId) => {
    if (!confirm("确定要删除这个任务吗？")) return;

    try {
      await api.deleteTask(taskId);
      setTasks((prev) => prev.filter((task) => task.id !== taskId));
    } catch (error) {
      console.error("Failed to delete task:", error);
      alert("删除失败，请重试");
    }
  };

  const handleNavigateToDeadline = (task) => {
    const deadlineDate = new Date(task.dueDate);
    onNavigateToDate(deadlineDate);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center gap-4 mb-6">
        <button
          onClick={onBack}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-2xl font-bold text-gray-800">
          {dateUtils.formatDate(selectedDate)}
        </h1>
        <div className="ml-auto flex items-center gap-2">
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-3 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            新建任务
          </button>
        </div>
      </div>

      <div className="space-y-6">
        {/* Task creation modal for personal tasks */}
        <TaskCreateModal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          defaultDeadline={() => {
            // default to selectedDate 23:59:59
            const d = new Date(selectedDate);
            d.setHours(23, 59, 59, 0);
            return formatLocalDateTime(d);
          }}
          onCreated={async () => {
            try {
              // reload tasks for the selected date
              const data = await api.getCalendarData(
                selectedDate.getFullYear(),
                selectedDate.getMonth() + 1
              );
              setTasks(data.tasks || []);
            } catch (err) {
              console.error("refresh after create failed", err);
            }
            setShowCreateModal(false);
          }}
        />
        {selectedTasks.created.length > 0 && (
          <div>
            <h2 className="text-lg font-semibold mb-4 flex items-center gap-2 text-gray-700">
              <div className="w-4 h-4 bg-lime-500 rounded-full"></div>
              任务创建 ({selectedTasks.created.length})
            </h2>
            <div className="space-y-3">
              {selectedTasks.created.map((task) => (
                <div
                  key={`created-${task.id}`}
                  className="bg-white border border-gray-200 rounded-lg p-4 cursor-pointer"
                  onClick={() => {
                    try {
                      if (taskEditor && taskEditor.open) {
                        taskEditor.open(task, async () => {
                          try {
                            const data = await api.getCalendarData(
                              selectedDate.getFullYear(),
                              selectedDate.getMonth() + 1
                            );
                            setTasks(data.tasks || []);
                          } catch (err) {
                            console.error("refresh after save failed", err);
                          }
                        });
                      }
                    } catch (err) {
                      console.error("open editor for created item failed", err);
                    }
                  }}
                >
                  <div className="flex items-center gap-2 text-sm text-gray-600 mb-2">
                    <span className="font-medium">创建任务:</span>
                    <span
                      className="font-medium"
                      style={{ color: task.courseColor }}
                    >
                      {task.courseName}
                    </span>
                    <span className="text-gray-800 font-semibold">
                      {task.title}
                    </span>
                  </div>
                  {task.description && (
                    <p className="text-gray-600 text-sm mb-2">
                      {task.description}
                    </p>
                  )}
                  <div className="text-xs text-gray-500">
                    截止时间: {dateUtils.formatDateTime(new Date(task.dueDate))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {selectedTasks.due.length > 0 && (
          <div>
            <h2 className="text-lg font-semibold mb-4 flex items-center gap-2 text-gray-700">
              <CalendarIcon size={20} className="text-red-500" />
              任务截止 ({selectedTasks.due.length})
            </h2>
            <div className="space-y-3">
              {/* Render editor for new task (editingTask empty object) */}
              {editingTask && !editingTask.id && (
                <TaskEditor
                  key={`new-task-${dateUtils.formatDateString(selectedDate)}`}
                  task={editingTask}
                  onSave={handleSaveTask}
                  onCancel={() => setEditingTask(null)}
                />
              )}

              {selectedTasks.due.map((task) =>
                editingTask?.id === task.id ? (
                  <TaskEditor
                    key={task.id}
                    task={editingTask}
                    onSave={handleSaveTask}
                    onCancel={() => setEditingTask(null)}
                  />
                ) : (
                  <TaskCard
                    key={task.id}
                    task={task}
                    onEdit={(t) => {
                      taskEditor.open(t, async () => {
                        try {
                          const data = await api.getCalendarData(
                            selectedDate.getFullYear(),
                            selectedDate.getMonth() + 1
                          );
                          setTasks(data.tasks);
                        } catch (err) {
                          console.error("refresh after save failed", err);
                        }
                      });
                    }}
                    onDelete={handleDeleteTask}
                    onNavigateToDeadline={handleNavigateToDeadline}
                  />
                )
              )}
            </div>
          </div>
        )}

        {selectedTasks.created.length === 0 &&
          selectedTasks.due.length === 0 && (
            <div className="text-center py-12 text-gray-500">
              <CalendarIcon size={48} className="mx-auto mb-4 opacity-50" />
              <p className="text-lg mb-2">这一天没有任务</p>
              <p className="text-sm">选择其他日期查看任务</p>
            </div>
          )}
      </div>
    </div>
  );
};

// Layout Components
const Header = ({ user, onMenuClick }) => {
  const { logout } = useAuth();

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4 sticky top-0 z-10">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={onMenuClick}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <Menu size={20} />
          </button>
          <h1 className="text-xl font-bold text-gray-800">任务管理系统</h1>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
              <User size={16} className="text-blue-600" />
            </div>
            <span className="text-sm font-medium text-gray-700">
              {user?.name}
            </span>
          </div>
          <button
            onClick={logout}
            className="text-sm text-gray-500 hover:text-gray-700 transition-colors px-3 py-1 rounded-md hover:bg-gray-100"
          >
            退出
          </button>
        </div>
      </div>
    </header>
  );
};

const Sidebar = ({ isOpen, onClose }) => {
  const { user } = useAuth();
  const taskEditor = useContext(TaskEditorContext);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState("");
  const [profileData, setProfileData] = useState(null);
  const [showSettings, setShowSettings] = useState(false);
  const [showJoinClass, setShowJoinClass] = useState(false);
  const [showCreateClass, setShowCreateClass] = useState(false);
  const [classes, setClasses] = useState([]);
  const [loadingClasses, setLoadingClasses] = useState(false);
  const [activeClass, setActiveClass] = useState(null);
  const [showClassModal, setShowClassModal] = useState(false);
  const [classTasks, setClassTasks] = useState([]);
  const [loadingClassTasks, setLoadingClassTasks] = useState(false);
  const [classCanCreate, setClassCanCreate] = useState(false);
  const [showCreateClassTaskModal, setShowCreateClassTaskModal] =
    useState(false);
  const [syncRange, setSyncRange] = useState("month");
  const [showMembersModal, setShowMembersModal] = useState(false);
  const [currentUserRole, setCurrentUserRole] = useState(null);
     // --- 新增：待处理审批功能的状态 ---
  const [pendingApprovals, setPendingApprovals] = useState({ list: [], total: 0 });
  const [loadingApprovals, setLoadingApprovals] = useState(false);
  const [showApprovalModal, setShowApprovalModal] = useState(false);

  const [showSearchClass, setShowSearchClass] = useState(false);

  // --- 新增功能所需的状态和引用 ---
  const [showInvitePopover, setShowInvitePopover] = useState(false);
  const [copyStatus, setCopyStatus] = useState("复制");
  const popoverRef = useRef(null);
  const triggerRef = useRef(null);
  useEffect(() => {
    const fetchApprovalCount = async () => {
      try {
        const res = await api.getPendingApprovals(0, 1); // 只需获取总数，请求一页一个即可
        setPendingApprovals(prev => ({ ...prev, total: res.total }));
      } catch (err) {
        console.error("Failed to fetch initial approval count", err);
      }
    };
    fetchApprovalCount();
  }, []);

  // --- 新增：打开审批模态框时，获取最新的完整列表 ---
  const handleOpenApprovalModal = async () => {
    setShowApprovalModal(true);
    setLoadingApprovals(true);
    try {
      const res = await api.getPendingApprovals();
      setPendingApprovals({ list: res.approvals, total: res.total });
    } catch (err) {
      console.error("Failed to load approvals list", err);
      setPendingApprovals({ list: [], total: 0 }); // 出错时清空
    } finally {
      setLoadingApprovals(false);
    }
  };

  // --- 新增：处理审批操作的核心函数 ---
  const handleProcessApproval = async (approvalToProcess, action) => {
    try {
      await api.processApproval(approvalToProcess.classInfo.id, approvalToProcess.applicant.userId, action);
      // 操作成功后，立即在前端更新状态，无需重新请求API
      setPendingApprovals(prev => ({
        // 从列表中移除已处理的项
        list: prev.list.filter(item => item.id !== approvalToProcess.id),
        // 总数减一
        total: prev.total - 1,
      }));
    } catch (err) {
      alert(`操作失败: ${err.message}`);
      // 可选：失败时重新加载列表以同步最新状态
      handleOpenApprovalModal();
    }
  };


  // --- 新增功能：处理点击外部区域关闭 Popover ---
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        showInvitePopover &&
        popoverRef.current &&
        !popoverRef.current.contains(event.target) &&
        triggerRef.current &&
        !triggerRef.current.contains(event.target)
      ) {
        setShowInvitePopover(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [showInvitePopover]);

  // --- 新增功能：处理复制邀请码的逻辑 ---
  const handleCopyInviteCode = () => {
    if (!activeClass?.inviteCode) return;
    navigator.clipboard.writeText(activeClass.inviteCode).then(
      () => {
        setCopyStatus("已复制!");
        setTimeout(() => {
          setCopyStatus("复制");
          setShowInvitePopover(false); // 复制成功后自动关闭 Popover
        }, 1500); // 1.5秒后恢复状态
      },
      (err) => {
        console.error("复制邀请码失败: ", err);
        setCopyStatus("失败");
        setTimeout(() => setCopyStatus("复制"), 2000);
      }
    );
  };

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoadingClasses(true);
      try {
        const res = await api.getClasses();
        if (mounted && res && Array.isArray(res.classes)) {
          setClasses(res.classes);
          // in background: fetch member counts for each class and merge into state
          (async () => {
            try {
              const promises = res.classes.map(async (c) => {
                try {
                  const headers = api.getAuthHeaders();
                  const resp = await fetch(
                    `${API_BASE}/classes/${c.id}/members?page=0&size=1`,
                    { headers }
                  );
                  const text = await resp.text();
                  const data = text ? JSON.parse(text) : {};
                  const payload = data?.data ?? data;
                  // try common places for total count
                  const total =
                    payload?.totalElements ?? payload?.total ??
                    (Array.isArray(payload?.content) ? payload.content.length : undefined) ??
                    (Array.isArray(payload) ? payload.length : undefined);
                  return { id: c.id, memberCount: typeof total === "number" ? total : undefined };
                } catch (err) {
                  console.warn("failed to fetch members for class", c.id, err);
                  return { id: c.id, memberCount: undefined };
                }
              });

              const results = await Promise.all(promises);
              // merge counts into classes state
              setClasses((prev) =>
                prev.map((pc) => {
                  const found = results.find((r) => r.id === pc.id);
                  if (!found) return pc;
                  // prefer existing pc.memberCount if present, else use fetched, else fallback to members array length or underscore fields
                  const mergedCount =
                    pc.memberCount ??
                    found.memberCount ??
                    (Array.isArray(pc.members) ? pc.members.length : undefined) ??
                    pc.member_count ??
                    pc.members_count ??
                    0;
                  return { ...pc, memberCount: mergedCount };
                })
              );
            } catch (err) {
              console.error("Failed to fetch class member counts", err);
            }
          })();
        }
      } catch (err) {
        console.error("Failed to load classes in Sidebar", err);
      } finally {
        setLoadingClasses(false);
      }
    };

    load();
    return () => {
      mounted = false;
    };
  }, []);

  // --- 新增功能：为班级详情模态框创建自定义标题 ---
  const renderClassModalTitle = () => {
    if (!activeClass) return "班级详情";

    return (
      <div className="flex items-center gap-2">
        <h3 className="text-lg font-semibold text-gray-800">
          {activeClass.name}
        </h3>
        {activeClass.inviteCode && (
          <div className="relative" ref={triggerRef}>
            <button
              onClick={() => setShowInvitePopover(!showInvitePopover)}
              title="查看邀请码"
              className="p-1 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100 transition-colors"
            >
              <Clipboard size={16} />
            </button>
            {showInvitePopover && (
              <div
                ref={popoverRef}
                className="absolute z-50 top-full left-1/2 -translate-x-1/2 mt-2 w-max bg-white border border-gray-200 rounded-lg shadow-xl p-3"
              >
                <div className="flex items-center gap-4">
                  <div>
                    <span className="text-xs text-gray-500">邀请码</span>
                    <p className="font-mono text-base text-gray-800">
                      {activeClass.inviteCode}
                    </p>
                  </div>
                  <button
                    onClick={handleCopyInviteCode}
                    className={`px-3 py-1.5 text-sm rounded-md transition-all duration-300 flex items-center gap-1.5 ${
                      copyStatus === "已复制!"
                        ? "bg-green-100 text-green-700"
                        : "bg-blue-500 text-white hover:bg-blue-600"
                    }`}
                  >
                    {copyStatus === "已复制!" ? (
                      <CheckCircle size={14} />
                    ) : (
                      <Clipboard size={14} />
                    )}
                    {copyStatus}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

  // --- 班级模态框关闭时的清理逻辑 ---
  const handleCloseClassModal = () => {
    setShowClassModal(false);
    setShowInvitePopover(false); // 确保 Popover 也被关闭
    setCopyStatus("复制"); // 重置复制按钮状态
  };

  return (
    <>
      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-20 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <div
        className={`
                fixed left-0 top-0 h-full bg-white border-r border-gray-200 z-30 transition-all duration-300
                ${isOpen ? "w-64" : "w-16"}
                lg:relative lg:z-0
            `}
      >
        <div className="p-4 h-full flex flex-col">
          {/* User section */}
          <div
            className={`flex items-center gap-3 mb-6 ${
              !isOpen && "justify-center"
            }`}
          >
            <button
              onClick={async () => {
                // ... (user profile logic remains unchanged)
                setShowProfileModal(true);
                setProfileError("");
                setProfileData(null);
                setProfileLoading(true);
                try {
                  const res = await api.getCurrentUser();
                  setProfileData(res.user || res);
                } catch (err) {
                  console.error("Failed to load current user profile", err);
                  setProfileError(err.message || "加载用户信息失败");
                } finally {
                  setProfileLoading(false);
                }
              }}
              className="flex items-center gap-3 focus:outline-none"
            >
              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                <User size={20} className="text-blue-600" />
              </div>
              {isOpen && (
                <div className="text-left">
                  <div className="font-medium text-gray-800">
                    {user?.name || user?.displayName || "用户"}
                  </div>
                  <div className="text-xs text-gray-500">
                    {user?.username || user?.email || ""}
                  </div>
                </div>
              )}
            </button>
          </div>

          <Modal
            isOpen={showProfileModal}
            onClose={() => setShowProfileModal(false)}
            title="我的资料"
          >
            {/* ... (user profile modal content remains unchanged) */}
            <div>
              {profileLoading ? (
                <div className="text-center py-6">加载中...</div>
              ) : profileError ? (
                <div className="text-sm text-red-600">{profileError}</div>
              ) : profileData ? (
                <div className="space-y-3 text-sm text-gray-700">
                  <div>
                    <span className="font-medium">姓名: </span>
                    {profileData.name || "-"}
                  </div>
                  <div>
                    <span className="font-medium">用户名: </span>
                    {profileData.username || profileData.login || "-"}
                  </div>
                  <div>
                    <span className="font-medium">邮箱: </span>
                    {profileData.email || "-"}
                  </div>
                  <div>
                    <span className="font-medium">角色: </span>
                    {profileData.role || profileData.roles || "-"}
                  </div>
                  <div>
                    <span className="font-medium">注册时间: </span>
                    {profileData.createdAt
                      ? String(profileData.createdAt)
                          .replace("T", " ")
                          .slice(0, 16)
                      : "-"}
                  </div>
                </div>
              ) : (
                <div className="text-sm text-gray-500">无用户信息</div>
              )}
            </div>
          </Modal>

          {/* Classes section */}
          <div className="flex-1 overflow-y-auto">
            {isOpen && (
              <h3 className="text-sm font-medium text-gray-500 mb-3">班级</h3>
            )}
            <div className="space-y-2">
              {loadingClasses && isOpen ? (
                <div className="text-sm text-gray-500">加载中...</div>
              ) : classes.length === 0 && isOpen ? (
                <div className="text-sm text-gray-500">
                  暂无班级，您可以创建或加入班级
                </div>
              ) : (
                classes.map((classItem) => (
                  <div
                    key={classItem.id}
                    className={`
                                                flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors
                                                ${!isOpen && "justify-center"}
                                            `}
                    title={!isOpen ? classItem.name : ""}
                    onClick={async () => {
                      if (!isOpen) return;
                      setActiveClass(classItem);
                      setShowClassModal(true);
                      setLoadingClassTasks(true);
                      setClassCanCreate(false);
                      try {
                        const res = await api.getClassTasks(classItem.id);
                        setClassTasks(res.tasks || []);
                        // check permission
                        try {
                          const perm = await api.checkClassPermission(
                            classItem.id
                          );
                          if (perm && perm.ok) {
                            setClassCanCreate(
                              !!perm.canCreateTask || !!perm.canCreate || false
                            );
                          } else {
                            const can = !!(
                              classItem.isCreator ||
                              classItem.isOwner ||
                              (classItem.currentUserRole &&
                                ["ADMIN", "TEACHER", "OWNER"].includes(
                                  String(
                                    classItem.currentUserRole
                                  ).toUpperCase()
                                ))
                            );
                            setClassCanCreate(can);
                          }
                          const permRes = await api.getCurrentUserPermissions(
                            classItem.id
                          );
                          if (permRes && permRes.permissions) {
                            setCurrentUserRole(permRes.permissions.role);
                          }
                        } catch (err) {
                          console.warn("class permission check failed", err);
                          const can = !!(
                            classItem.isCreator ||
                            classItem.isOwner ||
                            (classItem.currentUserRole &&
                              ["ADMIN", "TEACHER", "OWNER"].includes(
                                String(classItem.currentUserRole).toUpperCase()
                              ))
                          );
                          setClassCanCreate(can);
                          setCurrentUserRole(null);
                        }
                      } catch (err) {
                        console.error("Failed to load class tasks", err);
                      } finally {
                        setLoadingClassTasks(false);
                      }
                    }}
                  >
                    <div
                      className="w-4 h-4 rounded flex-shrink-0"
                      style={{ backgroundColor: classItem.color || "#cbd5e1" }}
                    />
                    {isOpen && (
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium text-gray-800 truncate">
                          {classItem.name}
                        </div>
                        <div className="text-xs text-gray-500">
                          {(
                            classItem.memberCount ??
                            classItem.membersCount ??
                            (Array.isArray(classItem.members)
                              ? classItem.members.length
                              : undefined) ??
                            classItem.member_count ??
                            classItem.members_count ??
                            0
                          )}{" "}
                          人
                        </div>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
          <div className="pt-4 border-t border-gray-200 space-y-1">
            <button
              onClick={handleOpenApprovalModal}
              className={`
                                flex items-center justify-between gap-3 p-2 rounded-lg hover:bg-gray-50 transition-colors w-full
                                ${!isOpen && "justify-center"}
                            `}
              title={!isOpen ? "待处理" : ""}
            >
              <div className="flex items-center gap-3">
                <Bell size={20} className="text-gray-500" />
                {isOpen && <span className="text-sm text-gray-700">待处理</span>}
              </div>
              {isOpen && pendingApprovals.total > 0 && (
                <span className="bg-red-500 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
                  {pendingApprovals.total}
                </span>
              )}
            </button>
            <button
              onClick={() => setShowSettings(true)}
              className={`
                                flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 transition-colors w-full
                                ${!isOpen && "justify-center"}
                            `}
              title={!isOpen ? "设置" : ""}
            >
              <Settings size={20} className="text-gray-500" />
              {isOpen && <span className="text-sm text-gray-700">设置</span>}
            </button>
          </div>
        </div>
      </div>

      {/* Settings Modal */}
      <Modal
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        title="设置"
      >
        {/* ... (settings modal content remains unchanged) */}
        <div className="space-y-3">
          <button
            onClick={() => {
              setShowSettings(false);
              setShowCreateClass(true);
            }}
            className="w-full text-left p-3 rounded-lg hover:bg-gray-50 flex items-center gap-3 border border-gray-200"
          >
            <Plus size={16} className="text-green-600" />
            <div>
              <div className="font-medium">创建班级</div>
              <div className="text-xs text-gray-500">创建新的班级</div>
            </div>
          </button>
          <button
            onClick={() => {
              // open search modal from settings
              setShowSettings(false);
              setShowSearchClass(true);
            }}
            className="w-full text-left p-3 rounded-lg hover:bg-gray-50 flex items-center gap-3 border border-gray-200"
          >
            <Search size={16} className="text-blue-600" />
            <div>
              <div className="font-medium">搜索加入</div>
              <div className="text-xs text-gray-500">按名称搜索公开班级并申请加入</div>
            </div>
          </button>
          <button
            onClick={() => {
              setShowSettings(false);
              setShowJoinClass(true);
            }}
            className="w-full text-left p-3 rounded-lg hover:bg-gray-50 flex items-center gap-3 border border-gray-200"
          >
            <BookOpen size={16} className="text-blue-600" />
            <div>
              <div className="font-medium">加入班级</div>
              <div className="text-xs text-gray-500">通过邀请码加入</div>
            </div>
          </button>
        </div>
      </Modal>

      {/* Class Search Modal */}
      <ClassSearchModal
        isOpen={showSearchClass}
        onClose={() => setShowSearchClass(false)}
        onRequestJoin={async (cls) => {
          // navigate to class detail or attempt join -- open class modal
          setShowSearchClass(false);
          setActiveClass(cls);
          setShowClassModal(true);
        }}
      />

      {/* Join Class Modal */}
      <Modal
        isOpen={showJoinClass}
        onClose={() => setShowJoinClass(false)}
        title="加入班级"
      >
        <JoinClassForm
          onClose={() => setShowJoinClass(false)}
          onJoined={(newClass) => {
            if (newClass) setClasses((prev) => [newClass, ...prev]);
          }}
        />
      </Modal>

      {/* Create Class Modal */}
      <Modal
        isOpen={showCreateClass}
        onClose={() => setShowCreateClass(false)}
        title="创建班级"
      >
        <CreateClassForm
          onClose={() => setShowCreateClass(false)}
          onCreated={(cls) => {
            if (cls) setClasses((prev) => [cls, ...prev]);
          }}
        />
      </Modal>

      {/* Class Detail Modal --- MODIFIED --- */}
      <Modal
        isOpen={showClassModal}
        onClose={handleCloseClassModal} // 使用新的关闭处理函数
        title={renderClassModalTitle()} // 使用新的自定义标题渲染函数
        size="lg"
      >
        <div>
          {loadingClassTasks ? (
            <div className="text-center py-6">加载中...</div>
          ) : (
            <div className="space-y-3">
              {classTasks.length === 0 ? (
                <div className="text-sm text-gray-500">该班级暂无任务</div>
              ) : (
                classTasks.map((t) => (
                  <div
                    key={t.id}
                    className="bg-white border rounded p-3 cursor-pointer"
                    onClick={() => {
                      try {
                        if (taskEditor && taskEditor.open) {
                          taskEditor.open(t, async () => {
                            const refreshed = await api.getClassTasks(
                              activeClass.id
                            );
                            setClassTasks(refreshed.tasks || []);
                          });
                        }
                      } catch (err) {
                        console.error(
                          "open editor from class modal failed",
                          err
                        );
                      }
                    }}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-medium">{t.title}</div>
                        <div className="text-xs text-gray-500">
                          {t.courseName} • 截止: {t.deadline}
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              )}

              <div className="pt-4 border-t flex items-center justify-between">
                <div>
                  <button
                    onClick={() => setShowMembersModal(true)}
                    className="px-3 py-2 bg-gray-200 text-gray-700 rounded"
                  >
                    成员
                  </button>
                </div>

                <div className="flex items-center gap-3">
                  <label className="text-sm text-gray-600">时间范围:</label>
                  <select
                    value={syncRange}
                    onChange={(e) => setSyncRange(e.target.value)}
                    className="px-2 py-1 border rounded"
                  >
                    <option value="day">day</option>
                    <option value="week">week</option>
                    <option value="month">month</option>
                    <option value="semester">semester</option>
                    <option value="year">year</option>
                  </select>
                  <div className="flex gap-2">
                    {classCanCreate && (
                      <button
                        onClick={() => {
                          setShowCreateClassTaskModal(true);
                        }}
                        title="新建班级任务"
                        className="px-3 py-2 bg-green-600 text-white rounded mr-2"
                      >
                        +
                      </button>
                    )}

                    <button
                      onClick={async () => {
                        if (!activeClass) return;
                        try {
                          const res = await api.syncClass(
                            activeClass.id,
                            syncRange
                          );
                          if (res.success) {
                            const refreshed = await api.getClassTasks(
                              activeClass.id
                            );
                            setClassTasks(refreshed.tasks || []);
                            alert(
                              "同步成功: " +
                                (res.result?.newlySyncedTasks ?? "0") +
                                " 个任务"
                            );
                            handleCloseClassModal();
                          } else {
                            alert("同步失败: " + (res.message || "未知错误"));
                          }
                        } catch (err) {
                          console.error("Sync error", err);
                          alert("同步发生异常: " + (err.message || "未知错误"));
                        }
                      }}
                      className="px-3 py-2 bg-blue-600 text-white rounded"
                    >
                      同步班级任务
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </Modal>

      <ClassMembersModal
        isOpen={showMembersModal}
        onClose={() => setShowMembersModal(false)}
        classItem={activeClass}
        currentUserRole={currentUserRole}
        currentUser={user}
      />

      <TaskCreateModal
        isOpen={showCreateClassTaskModal}
        onClose={() => setShowCreateClassTaskModal(false)}
        classId={activeClass ? activeClass.id : null}
        defaultDeadline={() => {
          const d = new Date();
          d.setDate(d.getDate() + 7);
          d.setHours(23, 59, 59, 0);
          return formatLocalDateTime(d);
        }}
        onCreated={async () => {
          try {
            const refreshed = await api.getClassTasks(activeClass.id);
            setClassTasks(refreshed.tasks || []);
          } catch (err) {
            console.error("refresh after class create failed", err);
          }
          setShowCreateClassTaskModal(false);
        }}
      />
      <ApprovalListModal
        isOpen={showApprovalModal}
        onClose={() => setShowApprovalModal(false)}
        approvals={pendingApprovals.list}
        onProcess={handleProcessApproval}
        loading={loadingApprovals}
      />
    </>
  );
};

const Layout = ({ children }) => {
  const { user } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gray-50">
      <Header user={user} onMenuClick={() => setSidebarOpen((s) => !s)} />

      <div className="flex">
        <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <main className="flex-1 overflow-hidden">{children}</main>
      </div>
    </div>
  );
};

// Main App Component
const TaskManagementApp = () => {
  const { isAuthenticated } = useAuth();
  const [authMode, setAuthMode] = useState("login");
  const [currentView, setCurrentView] = useState("calendar");
  const [selectedDate, setSelectedDate] = useState(null);

  const handleDateSelect = useCallback((date) => {
    setSelectedDate(date);
    setCurrentView("taskDetail");
  }, []);

  const handleBackToCalendar = useCallback(() => {
    setCurrentView("calendar");
    setSelectedDate(null);
  }, []);

  const handleNavigateToDate = useCallback((date) => {
    setSelectedDate(date);
    setCurrentView("taskDetail");
  }, []);

  if (!isAuthenticated) {
    // Allow direct verify-email landing page even when not authenticated
    if (typeof window !== 'undefined' && window.location && window.location.pathname && window.location.pathname.startsWith('/verify-email')) {
      return <VerifyEmailPage />;
    }

    return (
      <div>
        {authMode === "login" ? (
          <LoginForm onSwitchToRegister={() => setAuthMode("register")} />
        ) : (
          <RegisterForm onSwitchToLogin={() => setAuthMode("login")} />
        )}
      </div>
    );
  }

  return (
    <Layout>
      <div className="h-screen overflow-hidden">
        {currentView === "calendar" && (
          <div className="p-6 h-full overflow-y-auto">
            <CalendarView onDateSelect={handleDateSelect} />
          </div>
        )}

        {currentView === "taskDetail" && selectedDate && (
          <div className="h-full overflow-y-auto">
            <TaskDetail
              selectedDate={selectedDate}
              onBack={handleBackToCalendar}
              onNavigateToDate={handleNavigateToDate}
            />
          </div>
        )}
      </div>
    </Layout>
  );
};

// App with providers
const App = () => {
  useEffect(() => {
    console.log(
      "[frontend] App mounted, auth token:",
      localStorage.getItem("token")
    );
  }, []);

  return (
    <AuthProvider>
      <TaskEditorProvider>
        <TaskManagementApp />
      </TaskEditorProvider>
    </AuthProvider>
  );
};

export default App;
