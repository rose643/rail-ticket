package com.ahrose.railticket.train.base.service.impl;

import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.request.TrainCreateRequest;
import com.ahrose.railticket.bean.request.TrainUpdateRequest;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.base.service.TrainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * ClassName: TrainServiceImpl
 * Package: com.ahrose.railticket.train.base.service.impl
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/9/24 20:00
 * @Version 1.0
 */
@Service
public class TrainServiceImpl extends ServiceImpl<TrainMapper, Train> implements TrainService {


    @Autowired
    private TrainMapper trainMapper;

    @Override
    public List<Train> listTrains(String keyword) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(condition -> condition
                    .like(Train::getTrainNo, keyword)
                    .or()
                    .like(Train::getName, keyword));
        }

        wrapper.orderByAsc(Train::getTrainNo);
        return trainMapper.selectList(wrapper);
    }

    @Override
    public Train getTrainById(Long id) {
        return getTrainByIdOrThrow(id);
    }

    @Override
    public Train createTrain(TrainCreateRequest request) {
        if (request == null) {
            throw new BusinessException(400, "车次信息不能为空");
        }
        checkTrainNoNotExists(request.trainNo());

        Train train = new Train();
        train.setTrainNo(request.trainNo());
        train.setName(request.name());
        train.setStatus(1);
        int insert = trainMapper.insert(train);
        if (insert > 0) {
            return train;
        } else {
            throw new BusinessException(500, "创建车次失败");
        }
    }

    @Override
    public Train updateTrain(Long id, TrainUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(400, "更新车次信息不能为空");
        }

        Train train = getTrainByIdOrThrow(id);
        train.setName(request.name());
        int update = trainMapper.updateById(train);
        if (update > 0) {
            return train;
        } else {
            throw new BusinessException(500, "更新车次失败");
        }
    }

    @Override
    public Train updateTrainStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException(400, "车次状态不能为空");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "车次状态只能为 0 或 1");
        }

        Train train = getTrainByIdOrThrow(id);
        train.setStatus(status);
        int update = trainMapper.updateById(train);
        if (update > 0) {
            return train;
        } else {
            throw new BusinessException(500, "更新车次状态失败");
        }
    }

    /** 根据主键获取车次；不存在时统一抛出业务异常。 */
    private Train getTrainByIdOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }

        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BusinessException(404, "车次不存在");
        }
        return train;
    }

    /** 校验车次号未被占用；数据库唯一索引仍作为并发写入的最终兜底。 */
    private void checkTrainNoNotExists(String trainNo) {
        Long count = trainMapper.selectCount(new LambdaQueryWrapper<Train>()
                .eq(Train::getTrainNo, trainNo));
        if (count != null && count > 0) {
            throw new BusinessException(400, "车次号已存在");
        }
    }
}
