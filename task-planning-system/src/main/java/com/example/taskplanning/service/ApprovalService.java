package com.example.taskplanning.service;

import com.example.taskplanning.dto.GlobalApprovalResponseDto;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserClassRelation;
import com.example.taskplanning.repository.UserClassRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApprovalService {

    private final UserClassRelationRepository userClassRelationRepository;
    private final UserService userService;

    @Autowired
    public ApprovalService(UserClassRelationRepository userClassRelationRepository, UserService userService) {
        this.userClassRelationRepository = userClassRelationRepository;
        this.userService = userService;
    }

    /**
     * 获取当前管理员名下所有待处理的入班申请
     * @param pageable 分页参数
     * @return 分页的全局待审批DTO
     */
    public Page<GlobalApprovalResponseDto> getGlobalPendingApprovals(Pageable pageable) {
        // 1. 获取当前登录的用户实体
        User currentUser = userService.getCurrentUserEntity();

        // 2. 调用Repository中的新方法，传入当前用户ID进行查询
        Page<UserClassRelation> pendingRelations = userClassRelationRepository.findPendingApprovalsForManager(
                currentUser.getId(),
                UserClassRelation.JoinStatus.PENDING,
                pageable
        );

        // 3. 将查询结果 Page<UserClassRelation> 映射为 Page<GlobalApprovalResponseDto>
        return pendingRelations.map(GlobalApprovalResponseDto::new);
    }
}